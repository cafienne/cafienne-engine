/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.http;

import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class HTTPCall extends SubProcess<HTTPCallDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(HTTPCall.class);

    private final Result result = new Result(this);

    public HTTPCall(ProcessTaskActor processTask, HTTPCallDefinition definition) {
        super(processTask, definition);
    }

    @Override
    public void reactivate() {
        start(); // Just do the call again.
    }

    @Override
    public void start() {
        boolean successful = runCall();
        // Print debug information
        processTaskActor.addDebugInfo(result::getDebugInfo);
        // Set raw output parameters
        getRawOutputParameters().merge(result.toJSON());

        if (successful) {
            raiseComplete();
        } else {
            setFault(result.getException());
            raiseFault(result.getErrorDescription());
        }
    }

    private boolean runCall() {
        // Bind any parameters in the URL, any content and the http method to the input parameters of this task.
        URL targetURL = definition.getURL().resolveURL(processTaskActor);
        result.setTargetURL(targetURL);
        String requestMethod = definition.getMethod().resolve(processTaskActor);
        result.setRequestMethod(requestMethod);

        // Now fetch and open the URL
        HttpURLConnection httpConnection;
        DataOutputStream ostream;

        try {
            // First let's try to open the connection
            httpConnection = (HttpURLConnection) targetURL.openConnection();
        } catch (IOException failedToOpenURL) {
            return result.handleFailure("Could not create a connection with " + targetURL, failedToOpenURL);
        }

        // Fill in the http method. This is parameterized, and may fail, in which case we cannot continue with the http call and need to raise an error.
        try {
            httpConnection.setRequestMethod(requestMethod);
        } catch (IOException e) {
            return result.handleFailure("Cannot set http method " + requestMethod, e);
        }

        // Now fill the http headers
        // 1. Map headers to simple strings (does parameter substitution).
        Map<String, String> headers = new LinkedHashMap<>();
        definition.getHeaders(processTaskActor).forEach(header -> headers.put(header.getName(), header.getValue()));
        // Store the headers in the call status object for debugging purposes
        result.setRequestHeaders(headers);
        // Set the headers on the connection
        headers.forEach(httpConnection::setRequestProperty);

        if (requestMethod.equalsIgnoreCase("POST") || requestMethod.equalsIgnoreCase("PUT")) {
            String requestPayload = definition.getContent().resolve(processTaskActor).toString();
            result.setRequestPayload(requestPayload);

            // Only if there is any input to be posted to the URL will we setup an interactive connection and start writing the data
            if (requestPayload.trim().length() > 0) {
                httpConnection.setDoInput(true);
                httpConnection.setDoOutput(true);
                try {
                    ostream = new DataOutputStream(httpConnection.getOutputStream());
                    ostream.writeBytes(requestPayload);
                    ostream.flush();
                    ostream.close();
                } catch (IOException e) {
                    return result.handleFailure("Failed to write content to " + targetURL, e);
                }
            }
        }

        // Now start reading the response ...
        try {
            result.setResponseCode(httpConnection.getResponseCode());
            result.setResponseMessage(httpConnection.getResponseMessage());
            result.setResponseHeaders(httpConnection.getHeaderFields());
        } catch (IOException ioe) {
            return result.handleFailure("Failed to read response", ioe);
        }

        // ... and if it is not in the HTTP 200 range, we will read the error from the connection and raise it back into the plan-item (which will go "Failed")
        if (result.isOutOf200Range()) {
            StringBuilder errorMessage = new StringBuilder();
            InputStream errorStream = null;
            try {
                errorStream = httpConnection.getErrorStream();
                int c;
                while (errorStream != null && (c = errorStream.read()) != -1) {
                    errorMessage.append((char) c);
                }
                result.setResponsePayload(errorMessage.toString());
                return result.handleFailure("Status code " + result.getCode() + " is interpreted as a failure");
            } catch (IOException e) {
                result.setResponsePayload(errorMessage.toString());
                return result.handleFailure("Failed to read response payload for status code " + result.getCode(), e);
            } finally {
                try {
                    if (errorStream != null) {
                        errorStream.close();
                    }
                } catch (IOException e) {
                    logger.warn("Failed to close the error stream", e);
                }
            }
        } else { // ... in this path things are "HTTP_OK"
            // And we'll now start reading the content
            try {
                InputStream payloadStream = httpConnection.getInputStream();
                StringBuilder payload = new StringBuilder();
                int c;
                while ((c = payloadStream.read()) != -1) {
                    payload.append((char) c);
                }

                result.setResponsePayload(payload.toString());

                payloadStream.close();
            } catch (IOException e) {
                return result.handleFailure("Technical failure while reading http response although http code was " + result.getCode(), e);
            }
        }

        if (httpConnection != null) {
            httpConnection.disconnect();
        }

        return true;
    }

    @Override
    public void suspend() {
    }

    @Override
    public void terminate() {
    }

    @Override
    public void resume() {
    }
}
