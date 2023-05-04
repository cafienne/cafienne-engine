/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.cmmn.actorapi.command.team;

import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.json.ValueMap;

/**
 * Generic abstraction for commands relating to CaseTeam.
 */
public abstract class CaseTeamCommand extends CaseCommand {
    protected CaseTeamCommand(CaseUserIdentity user, String caseInstanceId) {
        super(user, caseInstanceId);
    }

    protected CaseTeamCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        super.validate(caseInstance);
        if (! caseInstance.getCurrentTeamMember().isOwner()) {
            throw new AuthorizationException("Only case team owners can perform this action");
        }
        validate(caseInstance.getCaseTeam());
    }

    protected abstract void validate(Team team) throws InvalidCommandException;

    @Override
    public void processCaseCommand(Case caseInstance) {
        process(caseInstance.getCaseTeam());
    }

    protected abstract void process(Team team);
}
