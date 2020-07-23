/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.http;

import org.cafienne.akka.actor.serialization.json.*;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HTTPCall extends SubProcess<HTTPCallDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(HTTPCall.class);

    private int responseCode = -1;
    private String responseMessage = "";
    private ValueMap responseHeaders = new ValueMap();
    private String responsePayload = "";

    public HTTPCall(ProcessTaskActor processTask, HTTPCallDefinition definition) {
        super(processTask, definition);
    }

    @Override
    public void reactivate() {
        start(); // Just do the call again.
    }

    @Override
    public void start() {
        ValueMap processInputParameters = processTaskActor.getMappedInputParameters();

        // Bind any parameters in the URL, any content and the http method to the input parameters of this task.
        URL targetURL = definition.getURL().resolveParameters(processInputParameters).toURL();
        String requestPayload = definition.getContent().resolveParameters(processInputParameters).toString();
        String httpMethod = definition.getMethod().resolveParameters(processInputParameters).toString();

        processTaskActor.addDebugInfo(() -> httpMethod + " " + targetURL.toString());


        // Now fetch and open the URL
        HttpURLConnection httpConnection = null;
        DataOutputStream ostream = null;

        try {
            // First let's try to open the connection
            httpConnection = (HttpURLConnection) targetURL.openConnection();
        } catch (IOException failedToOpenURL) {
            raiseException(new RuntimeException("Could not create a connection with " + targetURL, failedToOpenURL));
            return;
        }

        // Fill in the http method. This is parameterized, and may fail, in which case we cannot continue with the http call and need to raise an error.
        try {
            httpConnection.setRequestMethod(httpMethod);
        } catch (IOException e) {
            raiseException(new RuntimeException("Cannot set http method " + httpMethod, e));
            return;
        }

        // Now fill the http headers
        fillHttpHeaders(httpConnection, processInputParameters);

        if (httpMethod.equalsIgnoreCase("POST") || httpMethod.equalsIgnoreCase("PUT")) {
            // Only if there is any input to be posted to the URL will we setup an interactive connection and start writing the data
            if (requestPayload.trim().length() > 0) {
                processTaskActor.addDebugInfo(() -> "Payload\n" + requestPayload);
                httpConnection.setDoInput(true);
                httpConnection.setDoOutput(true);
                try {
                    ostream = new DataOutputStream(httpConnection.getOutputStream());
                    ostream.writeBytes(requestPayload);
                    ostream.flush();
                    ostream.close();
                } catch (IOException e) {
                    raiseException(new RuntimeException("Failed to write content to " + targetURL, e));
                    return;
                }
            }
        }

        // Now start reading the response ...
        try {
            responseCode = httpConnection.getResponseCode();
            setRawOutputParameter(HTTPCallDefinition.RESPONSE_CODE_PARAMETER, new LongValue(responseCode));

            responseMessage = httpConnection.getResponseMessage();
            setRawOutputParameter(HTTPCallDefinition.RESPONSE_MESSAGE_PARAMETER, new StringValue(responseMessage));

            // Here we explicitly convert the response headers to values;
            httpConnection.getHeaderFields().entrySet().forEach((entry) -> {
                String headerName = entry.getKey();
                List<String> values = entry.getValue();
                if (headerName == null) {
                    headerName = ""; // Sometimes, odd enough, there is a nameless header ...
                }
                // TODO: mostly, the headers returned are single, i.e., not a real array. Maybe then it is better
                //  to just convert the header to StringValue instead of to ValueList?
                responseHeaders.put(headerName, Value.convert(values));
            });
            setRawOutputParameter(HTTPCallDefinition.RESPONSE_HEADERS_PARAMETER, responseHeaders);
        } catch (IOException ioe) {
            raiseException(new RuntimeException("Failed to read response", ioe));
            return;
        }

        // ... and if it is not in the HTTP 200 range, we will read the error from the connection and raise it back into the plan-item (which will go "Failed")
        if (responseCode == -1 || responseCode > 299) { // -1 means we're not getting valid http
            StringBuilder errorMessage = new StringBuilder();
            InputStream errorStream = null;
            try {
                errorStream = httpConnection.getErrorStream();
                int c;
                while (errorStream != null && (c = errorStream.read()) != -1) {
                    errorMessage.append((char) c);
                }
                setOutputParameters(responseCode, responseMessage, errorMessage.toString());
                // Although there is no exception here, we still raise a fault, because the HTTP_CODE is not range 200-299
                raiseFault();
            } catch (IOException e) {
                setOutputParameters(responseCode, responseMessage, errorMessage.toString());
                raiseFault(e);
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

                responsePayload = payload.toString();

                setOutputParameters(responseCode, responseMessage, responsePayload);

                payloadStream.close();
                raiseComplete();
            } catch (IOException e) {
                raiseFault(new RuntimeException("Technical failure while reading http response although http code was " + responseCode, e));
            }
        }

        if (httpConnection != null) {
            httpConnection.disconnect();
        }

    }

    /**
     * When an exception happens, we still need to set raw output parameters in order to avoid parameter mappings
     * on those to fail. All raw outputs will get a default, empty value (depending on their type);
     * Depending on how far the http call could be executed they may or not have actual values.
     * @param e
     */
    private void raiseException(Exception e) {
        setRawOutputParameter(HTTPCallDefinition.RESPONSE_HEADERS_PARAMETER, responseHeaders);
        setOutputParameters(responseCode, responseMessage, responsePayload);
        raiseFault(e);
    }

    private void fillHttpHeaders(final HttpURLConnection connection, ValueMap processInputParameters) {
        // Now fill the http headers.
        definition.getHeaders().forEach((HTTPCallDefinition.Header header) ->
        {
            String headerName = header.getName(processInputParameters);
            String headerValue = header.getValue(processInputParameters);
            processTaskActor.addDebugInfo(() -> "Setting http header " + headerName + " to " + headerValue);
            connection.setRequestProperty(headerName, headerValue);
        });
    }

    private void setOutputParameters(int responseCode, String responseMessage, String responsePayload) {
        setRawOutputParameter(HTTPCallDefinition.RESPONSE_CODE_PARAMETER, new LongValue(responseCode));
        setRawOutputParameter(HTTPCallDefinition.RESPONSE_MESSAGE_PARAMETER, new StringValue(responseMessage));
        try {
            // Try to parse the response into a JSON structure if possible.
            //  Otherwise just put the raw string
            setRawOutputParameter(HTTPCallDefinition.RESPONSE_PAYLOAD_PARAMETER, JSONReader.parse(responsePayload));
        } catch (IOException | JSONParseFailure e) {
            setRawOutputParameter(HTTPCallDefinition.RESPONSE_PAYLOAD_PARAMETER, new StringValue(responsePayload));
        }
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
