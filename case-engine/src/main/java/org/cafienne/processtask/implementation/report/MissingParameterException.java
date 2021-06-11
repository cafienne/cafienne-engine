package org.cafienne.processtask.implementation.report;

import org.cafienne.actormodel.command.exception.InvalidCommandException;

public class MissingParameterException extends InvalidCommandException {
    public MissingParameterException(String msg) {
        super(msg);
    }
}
