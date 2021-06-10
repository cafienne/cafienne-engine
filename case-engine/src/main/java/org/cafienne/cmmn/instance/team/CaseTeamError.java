package org.cafienne.cmmn.instance.team;

import org.cafienne.actormodel.command.exception.InvalidCommandException;

/**
 * Thrown when an error is found while trying to modify the case team.
 *
 */
public class CaseTeamError extends InvalidCommandException {
    public CaseTeamError(String string) {
        super(string);
    }
}
