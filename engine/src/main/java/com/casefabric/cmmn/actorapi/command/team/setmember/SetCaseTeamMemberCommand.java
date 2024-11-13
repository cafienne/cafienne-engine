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

package com.casefabric.cmmn.actorapi.command.team.setmember;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.team.CaseTeamCommand;
import com.casefabric.cmmn.actorapi.command.team.CaseTeamMember;
import com.casefabric.cmmn.actorapi.command.team.CaseTeamMemberDeserializer;
import com.casefabric.cmmn.instance.team.CaseTeamError;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * Abstraction for commands on individual case team members
 *
 */
abstract class SetCaseTeamMemberCommand<M extends CaseTeamMember> extends CaseTeamCommand {
    protected final M newMember;

    protected SetCaseTeamMemberCommand(CaseUserIdentity user, String caseInstanceId, M newMember) {
        super(user, caseInstanceId);
        this.newMember = newMember;
    }

    protected SetCaseTeamMemberCommand(ValueMap json, CaseTeamMemberDeserializer<M> reader) {
        super(json);
        this.newMember = reader.readMember(json.with(Fields.member));
    }

    @Override
    public void validate(Team team) {
        if (! newMember.isOwner()) {
            // Check that this is not the last owner
            validateNotLastOwner(team);
        }
        // Check whether the roles are valid
        newMember.validateRolesExist(team.getDefinition());
    }

    protected void validateNotLastOwner(Team team) {
        CaseTeamMember currentMember = newMember.currentMember(team);
        if (currentMember != null) {
            if (currentMember.isOwner() && team.getOwners().size() == 1) {
                throw new CaseTeamError("Cannot remove the last case owner");
            }
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.member, newMember);
    }
}
