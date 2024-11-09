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

package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMemberDeserializer;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Set;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public abstract class CaseTeamMemberChanged<Member extends CaseTeamMember> extends CaseTeamMemberEvent<Member> {
    public CaseTeamMemberChanged(Team team, Member newInfo) throws CaseTeamError {
        super(team, newInfo);
    }

    public CaseTeamMemberChanged(ValueMap json, CaseTeamMemberDeserializer<Member> reader) {
        super(json, reader);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeMemberChangedEvent(generator);
    }

    protected void writeMemberChangedEvent(JsonGenerator generator) throws IOException {
        writeCaseTeamMemberEvent(generator);
    }
}
