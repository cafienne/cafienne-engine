package org.cafienne.processtask.implementation.mail;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;

public class InvalidMailAddressException extends InvalidCommandException {
    public InvalidMailAddressException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public InvalidMailAddressException(String msg) {
        super(msg);
    }
}
