package org.cafienne.cmmn.definition.casefile;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;

/**
 * Thrown when an error is found while trying to modify the case file.
 *
 */
public class CaseFileError extends InvalidCommandException {
    public CaseFileError(String string) {
        super(string);
    }
}
