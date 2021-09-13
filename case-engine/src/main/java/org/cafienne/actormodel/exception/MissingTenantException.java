package org.cafienne.actormodel.exception;

import org.cafienne.actormodel.command.ModelCommand;

/**
 * Every {@link ModelCommand} must have a tenant set.
 * If this is missing, then the command cannot be handled, and this exception is thrown.
 */
public class MissingTenantException extends InvalidCommandException {
    public MissingTenantException(String msg) {
        super(msg);
    }
}
