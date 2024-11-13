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

package com.casefabric.cmmn.test.assertions;

import com.casefabric.actormodel.exception.SerializedException;
import com.casefabric.actormodel.response.CommandFailure;
import com.casefabric.cmmn.test.CaseTestCommand;
import com.casefabric.cmmn.test.TestScript;

/**
 * Some assertions for failures coming back from the case.
 *
 */
public class FailureAssertion extends ModelTestCommandAssertion {
    public FailureAssertion(CaseTestCommand testCommand, String errorMessage) {
        super(testCommand, true, errorMessage);
    }

    public FailureAssertion(CaseTestCommand testCommand) {
        this(testCommand, "");
    }

    public void print() {
        TestScript.debugMessage(this.toString());
    }

    @Override
    public String toString() {
        return "Result of step " + testCommand.getActionNumber() +": " + testCommand.getActualFailure().toJson();
    }

    /**
     * Assert that the testCommand failure contains the expected exception
     * @param expectedExceptionClass
     * @return
     */
    public SerializedException assertException(Class<? extends Throwable> expectedExceptionClass) {
        CommandFailure response = testCommand.getActualFailure();
        if (response != null) {
            SerializedException t = response.exception();
            if (expectedExceptionClass.isAssignableFrom(t.getExceptionClass())) {
                return t;
            }
            else {
                raiseError("Case returned a command failure with exception of type "+t.getExceptionClass().getName()+", but we are expecting an exception of type "+expectedExceptionClass.getName());
                // Will not happen because 'raiseError' throws an exception
                return null;
            }
        }
        raiseError("Case did not return an exception");
        // Will not happen because 'raiseError' throws an exception
        return null;
    }

    /**
     * Asserts that the failure contains an exception of the specified class, and also that the message
     * inside the exception contains the expectedMessage.
     * @param expectedExceptionClass
     * @param expectedMessage
     * @return
     */
    public FailureAssertion assertException(Class<? extends Throwable> expectedExceptionClass, String expectedMessage) {
        assertException(expectedExceptionClass);
        assertException(expectedMessage);
        return this;
    }

    /**
     * Asserts that the exception in the command failure contains the expected message
     * @param expectedMessage
     * @return
     */
    public FailureAssertion assertException(String expectedMessage) {
        SerializedException exception = assertException(Exception.class);
        String message = exception.getMessage();
        if (! message.contains(expectedMessage)) {
            raiseError("Received a CommandFailure with an unexpected message.\nExpecting :" + expectedMessage+"\nReceived  :"+message);
        }
        return this;
    }
}
