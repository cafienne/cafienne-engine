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

import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.team.CaseTeamGroup;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Command to add a consent group to the case team
 */
@Manifest
public class SetCaseTeamGroup extends SetCaseTeamMemberCommand<CaseTeamGroup> {
    public SetCaseTeamGroup(CaseUserIdentity user, String caseInstanceId, CaseTeamGroup newMember) {
        super(user, caseInstanceId, newMember);
    }

    public SetCaseTeamGroup(ValueMap json) {
        super(json, CaseTeamGroup::deserialize);
    }

    @Override
    public void validate(Team team) {
        super.validate(team);

        // TODO: this needs furhter implementation


        // Check whether the roles are valid
        newMember.validateRolesExist(team.getDefinition());
    }

    @Override
    protected void process(Team team) {
        team.setGroup(newMember);
    }
}
