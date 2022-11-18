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
        this(testCommand, false);
    }

    protected ModelTestCommandAssertion(CaseTestCommand testCommand, boolean expectCommandFailure) {
        this.testCommand = testCommand;
        if (expectCommandFailure) {
            expectCommandFailure();
        } else {
            expectNoCommandFailure();
        }
    }

    public CaseTestCommand getTestCommand() {
        return testCommand;
    }

    private void expectNoCommandFailure() {
        if (testCommand.getActualFailure() != null) {
            throw new AssertionError("Unexpected failure in test step " + testCommand.getActionNumber() + " [" + testCommand.getActualCommand().getClass().getSimpleName() + "]\n" + testCommand.getActualFailure().exception());
        }
    }

    private void expectCommandFailure() {
        if (testCommand.getActualFailure() == null) {
            throw new AssertionError("Test script expected a failure from the case engine in step " + testCommand.getActionNumber() + " [" + testCommand.getActualCommand().getClass().getSimpleName() + "]");
        }
    }
}
