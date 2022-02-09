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
