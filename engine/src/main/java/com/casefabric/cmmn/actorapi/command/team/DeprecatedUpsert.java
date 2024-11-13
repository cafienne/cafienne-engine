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

package com.casefabric.cmmn.actorapi.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class DeprecatedUpsert extends CaseTeamCommand {
    private final UpsertMemberData memberData;

    public DeprecatedUpsert(CaseUserIdentity user, String caseInstanceId, UpsertMemberData member) {
        super(user, caseInstanceId);
        this.memberData = member;
    }

    public DeprecatedUpsert(ValueMap json) {
        super(json);
        this.memberData = UpsertMemberData.deserialize(json.with(Fields.member));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.member, memberData);
    }

    @Override
    protected void validate(Team team) throws InvalidCommandException {
        memberData.validateRolesExist(team.getDefinition());
        memberData.validateNotLastOwner(team);
    }

    @Override
    protected void process(Team team) {
        team.upsert(memberData);
    }
}
