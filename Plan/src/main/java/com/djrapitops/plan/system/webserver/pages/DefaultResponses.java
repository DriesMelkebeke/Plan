/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.system.webserver.pages;

import com.djrapitops.plan.system.webserver.response.errors.NotFoundResponse;
import com.djrapitops.plan.system.webserver.response.PromptAuthorizationResponse;
import com.djrapitops.plan.system.webserver.response.Response;

/**
 * //TODO Class Javadoc Comment
 *
 * @author Rsl1122
 */
public enum DefaultResponses {
    NOT_FOUND(
            new NotFoundResponse("Make sure you're accessing a link given by a command, Examples:</p>"
                    + "<p>/player/PlayerName<br>" +
                    "/server/ServerName</p>")
    ),
    BASIC_AUTH(PromptAuthorizationResponse.getBasicAuthResponse());

    private final Response response;

    DefaultResponses(Response response) {
        this.response = response;
    }

    public Response get() {
        return response;
    }
}