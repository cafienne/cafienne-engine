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

package com.casefabric.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.actorapi.command.team.CaseTeamMember;
import com.casefabric.cmmn.actorapi.command.team.CaseTeamMemberDeserializer;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class CaseTeamMemberEvent<Member extends CaseTeamMember> extends CaseTeamEvent {
    public final Member member;

    protected CaseTeamMemberEvent(Team team, Member newInfo) {
        super(team);
        this.member = newInfo;
    }

    protected CaseTeamMemberEvent(ValueMap json, CaseTeamMemberDeserializer<Member> reader) {
        super(json);
        this.member = reader.readMember(json.with(Fields.member));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeCaseTeamMemberEvent(generator);
    }

    protected void writeCaseTeamMemberEvent(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.member, member);
    }
}

