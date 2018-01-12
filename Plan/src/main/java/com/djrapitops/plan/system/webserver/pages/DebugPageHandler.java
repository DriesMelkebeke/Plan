/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.system.webserver.pages;

import com.djrapitops.plan.system.webserver.Request;
import com.djrapitops.plan.system.webserver.response.Response;
import com.djrapitops.plan.system.webserver.response.pages.DebugPageResponse;

import java.util.List;

/**
 * //TODO Class Javadoc Comment
 *
 * @author Rsl1122
 */
public class DebugPageHandler extends PageHandler {

    @Override
    public Response getResponse(Request request, List<String> target) {
        return new DebugPageResponse();
    }
}