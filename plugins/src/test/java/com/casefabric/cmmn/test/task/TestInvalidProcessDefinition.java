package com.casefabric.cmmn.test.task;

import com.casefabric.cmmn.definition.InvalidDefinitionException;
import com.casefabric.cmmn.test.TestScript;
import org.junit.Test;

public class TestInvalidProcessDefinition {

    @Test
    public void testInvalidPDFProcessDefinition() {
        // This tests that the process definition must contain proper output parameter names; it actually tests the http call definition output parameter names
        try {
            TestScript.getInvalidDefinition("testdefinition/task/process/invalidpdfprocessdefinition.xml");
            // Did not go to the catch block
            throw new AssertionError("Test failed, as the case definition is not invalid");
        } catch (InvalidDefinitionException e) {
            // This is supposed to happen indeed.
            String expectedErrorText = "Invalid mapping in process definition pdfreport: source parameter pdfReportDatas cannot be used; use one of [exception, pdfReportData]";
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
