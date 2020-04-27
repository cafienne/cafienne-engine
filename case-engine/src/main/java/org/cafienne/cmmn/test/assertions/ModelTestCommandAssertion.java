package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.test.ModelTestCommand;

public class ModelTestCommandAssertion<T extends ModelTestCommand> {
    protected final T testCommand;

    protected ModelTestCommandAssertion(T testCommand) {
        this(testCommand, false);
    }

    protected ModelTestCommandAssertion(T testCommand, boolean expectCommandFailure) {
        this.testCommand = testCommand;
        if (expectCommandFailure) {
            expectCommandFailure();
        } else {
            expectNoCommandFailure();
        }
    }

    public T getTestCommand() {
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
