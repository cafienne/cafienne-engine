/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.engine.processtask.implementation.http;

import org.cafienne.json.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Result {
    private URL targetURL;
    private String requestMethod;
    private Map<String, String> requestHeaders;
    private String requestPayload;

    private int responseCode = -1;
    private String responseMessage = "";
    private Map<String, List<String>> responseHeaders = new HashMap<>();
    private String responsePayload = "";

    private final HTTPCall call;

    Result(HTTPCall call) {
        this.call = call;
    }

    void setTargetURL(URL targetURL) {
        this.targetURL = targetURL;
    }

    void setRequestMethod(String method) {
        this.requestMethod = method;
    }

    void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    String errorDescription = "";
    Throwable cause;

    boolean handleFailure(String description, Throwable cause) {
        this.cause = cause;
        return handleFailure(description);
    }

    boolean handleFailure(String description) {
        this.errorDescription = description;
        return false;
    }

    boolean isOutOf200Range() {
        return responseCode == -1 || responseCode > 299;
    }

    int getCode() {
        return responseCode;
    }

    String getErrorDescription() {
        return errorDescription;
    }

    Value<?> getException() {
        if (cause != null) {
            return Value.convert(cause);
        } else {
            return new ValueMap("description", getErrorDescription(), "response", getResponseDebugInfo());
        }
    }

    ValueMap toJSON() {
        ValueMap responseJson = new ValueMap();

        responseJson.put(HTTPCallDefinition.RESPONSE_HEADERS_PARAMETER, convertHeadersToJSON());
        responseJson.put(HTTPCallDefinition.RESPONSE_CODE_PARAMETER, new LongValue(responseCode));
        responseJson.put(HTTPCallDefinition.RESPONSE_MESSAGE_PARAMETER, new StringValue(responseMessage));
        responseJson.put(HTTPCallDefinition.RESPONSE_PAYLOAD_PARAMETER, convertPayloadToJSON(responsePayload));
        return responseJson;
    }

    Value<?> getDebugInfo() {
        return new ValueMap("Request", getRequestDebugInfo(), "Response", getResponseDebugInfo());
    }

    ValueMap getRequestDebugInfo() {
        ValueMap requestDebugInfo = new ValueMap();
        requestDebugInfo.put("url", new StringValue(requestMethod + " " + targetURL.toString()));
        if (! requestHeaders.isEmpty()) {
            requestDebugInfo.put("headers", Value.convert(requestHeaders));
        }
        if (requestPayload != null) {
            requestDebugInfo.put("payload", convertPayloadToJSON(requestPayload));
        }
        return requestDebugInfo;
    }

    ValueMap getResponseDebugInfo() {
        ValueMap headersConcatenated = new ValueMap(); // Convert header values to concatenated spaced string for ease of reading
        responseHeaders.forEach((headerName, headerValue) -> headersConcatenated.plus(headerName == null ? "" : headerName, String.join(" ", headerValue)));
        ValueMap responseDebugInfo = new ValueMap("code", responseCode, "message", new StringValue(responseMessage), "headers", headersConcatenated, "content", convertPayloadToJSON(responsePayload));
        if (responseCode == -1 && ! errorDescription.isEmpty()) {
            responseDebugInfo.put("error", new StringValue(errorDescription));
        }
        return responseDebugInfo;
    }

    private Value<?> convertPayloadToJSON(String payload) {
        try {
            // Try to parse the response into a JSON structure if possible.
            //  Otherwise just put the raw string
            return JSONReader.parse(payload);
        } catch (IOException | JSONParseFailure e) {
            return new StringValue(payload);
        }
    }

    private ValueMap convertHeadersToJSON() {
        ValueMap headersOutputParameter = new ValueMap();
        // Convert headers to a JSON ValueMap of { "headerName" : [val1, val2] }.
        //  Note: sometimes, odd enough, there is a nameless header ...
        responseHeaders.forEach((headerName, headerValue) -> headersOutputParameter.plus(headerName == null ? "" : headerName, headerValue));
        return headersOutputParameter;
    }
}
