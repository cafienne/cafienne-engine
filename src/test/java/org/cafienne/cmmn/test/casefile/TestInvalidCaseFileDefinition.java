/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.casefile;

import org.cafienne.cmmn.definition.InvalidDefinitionException;
import org.cafienne.cmmn.repository.MissingDefinitionException;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.infrastructure.Cafienne;
import org.junit.Test;

public class TestInvalidCaseFileDefinition {
    // Simple test for casefile structure
    @Test
    public void testInvalidCaseDefinition() {
        // First, test with an invalid Definitions document
        try {
            TestScript.getInvalidDefinition("testdefinition/casefile/invalidcasefiledefinition.xml");
            // Did not go to the catch block
            throw new AssertionError("Test failed, as the case definition is not invalid");
        } catch (InvalidDefinitionException e) {
            // This is supposed to happen indeed.
            String expectedErrorText = "The case file item 'Customer' refers to a definition named CustomerDefinition, but that definition is not found";
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

    @Test
    public void testMissingDefinition() {
        // First, test with an invalid Definitions document
        try {
            Cafienne.config().repository().DefinitionProvider().read(null, null, "a file that does not exist");
            // Did not go to the catch block
            throw new AssertionError("Test failed, as the case definition is not invalid");
        } catch (MissingDefinitionException e) {
            // Everything went fine
        } catch (InvalidDefinitionException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testBadSpelExpressionShouldFail() {
        // This tests a casefile that has a wrong spel expression (which you could have typed in the editor)
        try {
            TestScript.getInvalidDefinition("testdefinition/casefile/bad-spel-expression.xml");
            // Did not go to the catch block
            throw new AssertionError("Test failed, as the case definition is not invalid");
        } catch (InvalidDefinitionException e) {
            // This is supposed to happen indeed.
            String expectedErrorText = "Problem parsing right operand";
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

    @Test
    public void testEmptySpelExpressionShouldFail() {
        // This tests a casefile that has a wrong spel expression (which you could have typed in the editor)
        try {
            TestScript.getInvalidDefinition("testdefinition/casefile/empty-spel-expression.xml");
            // Did not go to the catch block
            throw new AssertionError("Test failed, as the case definition is not invalid");
        } catch (InvalidDefinitionException e) {
            // This is supposed to happen indeed.
            String expectedErrorText = "The parameter mapping in ProcessTask 'pid_cm_csTKE_40' has an empty expression";
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
