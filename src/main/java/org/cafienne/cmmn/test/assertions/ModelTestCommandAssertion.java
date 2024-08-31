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

package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.test.CaseTestCommand;

public class ModelTestCommandAssertion {
    protected final CaseTestCommand testCommand;

    protected ModelTestCommandAssertion(CaseTestCommand testCommand) {
        this(testCommand, false, "");
    }

    protected ModelTestCommandAssertion(CaseTestCommand testCommand, boolean expectCommandFailure, String errorMessage) {
        this.testCommand = testCommand;
        if (expectCommandFailure) {
            expectCommandFailure(errorMessage);
        } else {
            expectNoCommandFailure();
        }
    }

    public CaseTestCommand getTestCommand() {
        return testCommand;
    }

    private void expectNoCommandFailure() {
        if (testCommand.getActualFailure() != null) {
            raiseError("Received unexpected failure: " + testCommand.getActualFailure().exception());
        }
    }

    private void expectCommandFailure(String errorMessage) {
        if (testCommand.getActualFailure() == null) {
            raiseError(errorMessage);
        }
    }

    protected void raiseError(String errorMessage) {
        if (!errorMessage.trim().isEmpty()) {
            errorMessage = "\n  Expected error: " + errorMessage;
        }
        throw new AssertionError("Test script failed in step " + testCommand.getActionNumber() + " [user=" + testCommand.getUser().id() + "|command=" + testCommand.getActualCommand().getClass().getSimpleName() + "]\n" + errorMessage);
    }
}
