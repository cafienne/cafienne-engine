package org.cafienne.processtask.implementation.mail;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;

public class InvalidMailException extends InvalidCommandException {
    public InvalidMailException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
