package org.cafienne.processtask.implementation.report;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;

public class MissingParameterException extends InvalidCommandException {
    public MissingParameterException(String msg) {
        super(msg);
    }
}
