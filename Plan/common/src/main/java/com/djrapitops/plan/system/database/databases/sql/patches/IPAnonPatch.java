/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.system.database.databases.sql.patches;

import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.api.exceptions.database.DBOpException;
import com.djrapitops.plan.data.container.GeoInfo;
import com.djrapitops.plan.system.database.databases.sql.SQLDB;
import com.djrapitops.plan.system.database.databases.sql.processing.ExecStatement;
import com.djrapitops.plan.system.database.databases.sql.processing.QueryStatement;
import com.djrapitops.plan.system.database.databases.sql.tables.GeoInfoTable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IPAnonPatch extends Patch {

    private String tableName;
    private String tempTableName;

    public IPAnonPatch(SQLDB db) {
        super(db);
        tableName = GeoInfoTable.TABLE_NAME;
        tempTableName = "plan_ips_temp";
    }

    @Override
    public boolean hasBeenApplied() {
        return !containsUnAnonymizedIPs() && !hasTable(tempTableName);
    }

    private Boolean containsUnAnonymizedIPs() {
        String sql = "SELECT * FROM " + tableName +
                " WHERE " + GeoInfoTable.Col.IP + " NOT LIKE ? LIMIT 1";

        return query(new QueryStatement<Boolean>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, "%x%");
            }

            @Override
            public Boolean processResults(ResultSet set) throws SQLException {
                return set.next();
            }
        });
    }

    @Override
    public void apply() {
        Map<UUID, List<GeoInfo>> allGeoInfo = db.getGeoInfoTable().getAllGeoInfo();
        anonymizeIPs(allGeoInfo);
        groupHashedIPs();
    }

    private void anonymizeIPs(Map<UUID, List<GeoInfo>> allGeoInfo) {
        String sql = "UPDATE " + GeoInfoTable.TABLE_NAME + " SET " +
                GeoInfoTable.Col.IP + "=?, " +
                GeoInfoTable.Col.IP_HASH + "=? " +
                "WHERE " + GeoInfoTable.Col.IP + "=?";

        db.executeBatch(new ExecStatement(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                for (List<GeoInfo> geoInfos : allGeoInfo.values()) {
                    for (GeoInfo geoInfo : geoInfos) {
                        addToBatch(statement, geoInfo);
                    }
                }
            }

            private void addToBatch(PreparedStatement statement, GeoInfo geoInfo) throws SQLException {
                try {
                    String oldIP = geoInfo.getIp();
                    if (oldIP.endsWith(".xx.xx") || oldIP.endsWith("xx..")) {
                        return;
                    }
                    GeoInfo updatedInfo = new GeoInfo(
                            InetAddress.getByName(oldIP),
                            geoInfo.getGeolocation(),
                            geoInfo.getDate()
                    );
                    statement.setString(1, updatedInfo.getIp());
                    statement.setString(2, updatedInfo.getIpHash());
                    statement.setString(3, geoInfo.getIp());
                    statement.addBatch();
                } catch (UnknownHostException | NoSuchAlgorithmException ignore) {
                    // This ip is already anonymised or completely unusable.
                }
            }
        });
    }

    private void groupHashedIPs() {
        try {
            if (!hasTable(tempTableName)) {
                tempOldTable();
            }
            db.getGeoInfoTable().createTable();

            boolean hasUserIdColumn = hasColumn(tempTableName, "user_id");
            String identifiers = hasUserIdColumn ? "user_id" : "id, uuid";

            db.execute("INSERT INTO plan_ips (" +
                    identifiers + ", ip, ip_hash, geolocation, last_used" +
                    ") SELECT " +
                    identifiers + ", ip, ip_hash, geolocation, MAX(last_used) FROM plan_ips_temp GROUP BY ip_hash, " +
                    (hasUserIdColumn ? "user_id" : "uuid") +
                    ", ip, geolocation");
            dropTable(tempTableName);
        } catch (DBInitException e) {
            throw new DBOpException(e.getMessage(), e);
        }
    }

    private void tempOldTable() {
        if (!hasTable(tempTableName)) {
            renameTable(tableName, tempTableName);
        }
    }
}
