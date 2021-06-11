package org.cafienne.cmmn.instance.task.validation;

import org.cafienne.json.*;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.processtask.implementation.http.HTTPCallDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class TaskOutputValidator extends CMMNElement<HTTPCallDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(TaskOutputValidator.class);

    private final HTTPCallDefinition definition;
    private final HumanTask task;


    public TaskOutputValidator(HTTPCallDefinition httpCallDefinition, HumanTask task) {
        super(task, httpCallDefinition);
        this.definition = httpCallDefinition;
        this.task = task;
    }

    public ValidationResponse validate(ValueMap potentialTaskOutput) {
        ValueMap output = new ValueMap();

        int responseCode = -1;
        String responseMessage;
        Map<String, List<String>> responseHeaders;
        String responsePayload;


//        System.out.println("\n\nCREATING NEW VALIDATION CALL");

        ValueMap requestPayloadJson = new ValueMap(
                "task-output", potentialTaskOutput
                ,"metadata", new ValueMap(
                    "caseInstanceId", task.getCaseInstance().getId()
                    ,"taskId", task.getId()
                    ,"taskName", task.getName()
                    ,"user", task.getCaseInstance().getCurrentUser().toValue()
                )
//                ,"caseFile", task.getCaseInstance().getCaseFile().toJson()
        );

        // Bind any parameters in the URL, any content and the http method to the input parameters of this task.
        URL targetURL = definition.getURL().resolveParameters(requestPayloadJson).toURL();
        String requestPayload = requestPayloadJson.toString();
        String httpMethod = definition.getMethod().resolveParameters(requestPayloadJson).toString();

        addDebugInfo(() -> "Invoking task validation on output of task " + task.getName() + "[" + task.getId() + "] with " + httpMethod.toUpperCase() + " " + targetURL.toString() + " with ", requestPayloadJson);

        // Now fetch and open the URL
        HttpURLConnection httpConnection = null;
        DataOutputStream ostream = null;

        try {
            // First let's try to open the connection
            httpConnection = (HttpURLConnection) targetURL.openConnection();
        } catch (IOException failedToOpenURL) {
            return new ValidationError("Could not create a connection with " + targetURL, failedToOpenURL);
        }

        try {
            // Fill in the http method. This is parameterized, and may fail, in which case we cannot continue with the http call and need to raise an error.
            try {
                httpConnection.setRequestMethod(httpMethod);
            } catch (IOException e) {
                return new ValidationError("Cannot set http method " + httpMethod, e);
            }

            // Now fill the http headers
            fillHttpHeaders(httpConnection, requestPayloadJson);

            if (httpMethod.equalsIgnoreCase("POST") || httpMethod.equalsIgnoreCase("PUT")) {
                // Only if there is any input to be posted to the URL will we setup an interactive connection and start writing the data
                if (requestPayload.trim().length() > 0) {
                    addDebugInfo(() -> "Payload\n" + requestPayload);
                    httpConnection.setDoInput(true);
                    httpConnection.setDoOutput(true);
                    try {
                        ostream = new DataOutputStream(httpConnection.getOutputStream());
                        ostream.writeBytes(requestPayload);
                        ostream.flush();
                        ostream.close();
                    } catch (ConnectException e) {
                        return new ValidationError("Cannot establish connection to " + targetURL, e);
                    } catch (IOException e) {
                        return new ValidationError("Failed to write content to " + targetURL, e);
                    }
                }
            }

            // Now start reading the response ...
            try {
                responseCode = httpConnection.getResponseCode();
                output.put(HTTPCallDefinition.RESPONSE_CODE_PARAMETER, new LongValue(responseCode));

                responseMessage = httpConnection.getResponseMessage();
                output.put(HTTPCallDefinition.RESPONSE_MESSAGE_PARAMETER, new StringValue(responseMessage));

                // Here we explicitly convert the response headers to values;
                ValueMap convertedResponseHeaders = output.with(HTTPCallDefinition.RESPONSE_HEADERS_PARAMETER);
                responseHeaders = httpConnection.getHeaderFields();
                responseHeaders.entrySet().forEach((entry) -> {
                    String headerName = entry.getKey();
                    List<String> values = entry.getValue();
                    if (headerName == null) {
                        headerName = ""; // Sometimes, odd enough, there is a nameless header ...
                    }
                    // TODO: mostly, the headers returned are single, i.e., not a real array. Maybe then it is better
                    //  to just convert the header to StringValue instead of to ValueList?
                    convertedResponseHeaders.put(headerName, Value.convert(values));
                });
            } catch (IOException ioe) {
                return new ValidationError("Failed to read response", ioe);
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
//                    System.out.println("ERROR Responded "+responseCode+", with "+responseMessage+" and content:\n"+errorMessage);

                    output.put(HTTPCallDefinition.RESPONSE_PAYLOAD_PARAMETER, new StringValue(errorMessage.toString()));

                    // Although there is no exception here, we still raise a fault, because the HTTP_CODE is not range 200-299
                    return new ValidationError(output, new Exception("Unexpected http response code " + responseCode));
                } catch (IOException e) {
                    output.put(HTTPCallDefinition.RESPONSE_PAYLOAD_PARAMETER, new StringValue(errorMessage.toString()));
                    return new ValidationError(output, e);
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
                    ValueMap responseJSON = JSONReader.parse(httpConnection.getInputStream());
                    output.put(HTTPCallDefinition.RESPONSE_PAYLOAD_PARAMETER, responseJSON);
                    addDebugInfo(() -> "Response to validation of task " + task.getId() + ":", responseJSON);
                    return new ValidationResponse(responseJSON);
                } catch (IOException | JSONParseFailure e) {
                    return new ValidationError("Technical failure while reading http response although http code was " + responseCode, e);
                }
            }
        } finally {
            httpConnection.disconnect();
        }
    }

    private void fillHttpHeaders(final HttpURLConnection connection, ValueMap processInputParameters) {
        // Now fill the http headers.
        definition.getHeaders().forEach((HTTPCallDefinition.Header header) ->
        {
            String headerName = header.getName(processInputParameters);
            String headerValue = header.getValue(processInputParameters);
            addDebugInfo(() -> "Setting http header " + headerName + " to " + headerValue);
            connection.setRequestProperty(headerName, headerValue);
        });
    }
}
