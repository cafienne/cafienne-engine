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

package org.cafienne.engine.cmmn.actorapi.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.engine.cmmn.instance.team.CaseTeamError;
import org.cafienne.engine.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Command to set the case team
 */
@Manifest
public class SetCaseTeam extends CaseTeamCommand {

    private final CaseTeam caseTeam;

    public SetCaseTeam(CaseUserIdentity user, String caseInstanceId, CaseTeam caseTeam) {
        super(user, caseInstanceId);
        this.caseTeam = caseTeam;
    }

    public SetCaseTeam(ValueMap json) {
        super(json);
        this.caseTeam = CaseTeam.deserialize(json.with(Fields.team));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.team, caseTeam);
    }

    @Override
    public void validate(Team team) {
        // New team cannot be empty
        if (caseTeam.isEmpty()) throw new CaseTeamError("The new case team cannot be empty");
        // New team also must have owners
        if (caseTeam.owners().isEmpty()) throw new CaseTeamError("The new case team must have owners");
        // New team roles must match the case definition
        caseTeam.validate(team.getDefinition());
    }

    @Override
    protected void process(Team team) {
        team.replace(this.caseTeam);
    }
}
