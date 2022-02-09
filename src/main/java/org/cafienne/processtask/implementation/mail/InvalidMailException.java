package org.cafienne.processtask.implementation.mail;

import org.cafienne.actormodel.exception.InvalidCommandException;

public class InvalidMailException extends InvalidCommandException {
    public InvalidMailException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
