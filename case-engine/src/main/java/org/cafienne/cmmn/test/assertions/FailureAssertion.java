package org.cafienne.cmmn.test.assertions;

import org.cafienne.akka.actor.command.exception.SerializedException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.cmmn.test.ModelTestCommand;

/**
 * Some assertions for failures coming back from the case.
 *
 */
public class FailureAssertion extends ModelTestCommandAssertion {
    public FailureAssertion(ModelTestCommand testCommand) {
        super(testCommand, true);
        if (testCommand.getActualFailure() == null) {
            throw new AssertionError("Test script expected a failure from the engine in step " + testCommand.getActionNumber() + " [" + testCommand.getActualCommand().getClass().getSimpleName() + "]");
        }
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
                throw new AssertionError("Case returned a command failure with exception of type "+t.getExceptionClass().getName()+", but we are expecting an exception of type "+expectedExceptionClass.getName());
            }
        }
        throw new AssertionError("Case did not return an exception");
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
            throw new AssertionError("Received a CommandFailure with an unexpected message. Expecting " + expectedMessage+", received "+message);
        }
        return this;
    }
}
