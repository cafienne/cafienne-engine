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

package org.cafienne.engine.cmmn.actorapi.command.team.removemember;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.engine.cmmn.actorapi.command.team.CaseTeamCommand;
import org.cafienne.engine.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.engine.cmmn.instance.team.CaseTeamError;
import org.cafienne.engine.cmmn.instance.team.MemberType;
import org.cafienne.engine.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Abstraction for removing  individual case team members
 *
 */
abstract class RemoveCaseTeamMemberCommand<M extends CaseTeamMember> extends CaseTeamCommand {
    protected final String memberId;

    protected RemoveCaseTeamMemberCommand(CaseUserIdentity user, String caseInstanceId, String memberId) {
        super(user, caseInstanceId);
        this.memberId = memberId;
    }

    protected RemoveCaseTeamMemberCommand(ValueMap json) {
        super(json);
        this.memberId = json.readString(Fields.memberId);
    }

    protected abstract MemberType type();

    @Override
    public void validate(Team team) throws InvalidCommandException {
        CaseTeamMember member = member(team);
        if (member == null) {
            throw new CaseTeamError("The case team does not have a " + type() + " with id " + memberId);
        }
        validateNotLastOwner(team);
    }

    protected void validateNotLastOwner(Team team) {
        CaseTeamMember member = member(team);
        if (member != null) {
            if (member.isOwner() && team.getOwners().size() == 1) {
                throw new CaseTeamError("Cannot remove the last case owner");
            }
        }
    }

    protected abstract M member(Team team);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.memberId, memberId);
    }
}
