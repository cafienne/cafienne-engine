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

package com.casefabric.cmmn.actorapi.event.team.group;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.actorapi.command.team.CaseTeamGroup;
import com.casefabric.cmmn.actorapi.event.team.CaseTeamMemberChanged;
import com.casefabric.cmmn.instance.team.CaseTeamError;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamGroupChanged extends CaseTeamMemberChanged<CaseTeamGroup> {
    public CaseTeamGroupChanged(Team team, CaseTeamGroup newInfo) throws CaseTeamError {
        super(team, newInfo);
    }

    public CaseTeamGroupChanged(ValueMap json) {
        super(json, CaseTeamGroup::deserialize);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamMemberEvent(generator);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
