package org.cafienne.akka.actor.command.exception;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.ModelActor;

/**
 * This exception is typically raised during the {@link ModelCommand#validate(ModelActor)} method.
 * The case instance checks for this exception around its invocation of the validate method.
 *
 */
public class InvalidCommandException extends CommandException {
    public InvalidCommandException(String msg) {
        super(msg);
    }
    
    public InvalidCommandException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    public InvalidCommandException(Throwable cause) {
        super(cause);
    }

}
