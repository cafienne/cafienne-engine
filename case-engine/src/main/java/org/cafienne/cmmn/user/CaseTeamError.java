package org.cafienne.cmmn.user;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;

/**
 * Thrown when an error is found while trying to modify the case team.
 *
 */
public class CaseTeamError extends InvalidCommandException {
    CaseTeamError(String string) {
        super(string);
    }
}
