package org.cafienne.cmmn.test.task;

import org.cafienne.cmmn.definition.InvalidDefinitionException;
import org.cafienne.cmmn.test.TestScript;
import org.junit.Test;

public class TestInvalidProcessDefinition {
    @Test
    public void testInvalidHTTPProcessDefinition() {
        // This tests that the process definition must contain proper output parameter names; it actually tests the http call definition output parameter names
        try {
            TestScript.getInvalidDefinition("testdefinition/task/process/invalidhttpprocessdefinition.xml");
            // Did not go to the catch block
            throw new AssertionError("Test failed, as the case definition is not invalid");
        } catch (InvalidDefinitionException e) {
            // This is supposed to happen indeed.
            String expectedErrorText = "Invalid mapping in process definition GetDetailsWebService: source parameter responsesPayload cannot be used; use one of [exception, responseHeaders, responsePayload, responseMessage, responseCode]";
            for (String error : e.getErrors()) {
                if (error.contains(expectedErrorText)) {
                    // Found the error, so it's ok.
                    return;
                }
            }
            // Did not encounter error text
            throw new AssertionError("Test failed, as the error with text " + expectedErrorText + " was not encountered", e);
        }
    }
}
