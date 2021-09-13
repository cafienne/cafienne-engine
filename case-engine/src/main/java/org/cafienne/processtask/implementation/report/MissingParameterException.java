package org.cafienne.processtask.implementation.report;

import org.cafienne.actormodel.exception.InvalidCommandException;

public class MissingParameterException extends InvalidCommandException {
    public MissingParameterException(String msg) {
        super(msg);
    }
}
