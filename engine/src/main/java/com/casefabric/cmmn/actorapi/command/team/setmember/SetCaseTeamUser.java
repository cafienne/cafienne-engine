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
import com.casefabric.cmmn.actorapi.command.team.CaseTeamUser;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Command to add a user to the case team
 */
@Manifest
public class SetCaseTeamUser extends SetCaseTeamMemberCommand<CaseTeamUser> {
    public SetCaseTeamUser(CaseUserIdentity user, String caseInstanceId, CaseTeamUser newMember) {
        super(user, caseInstanceId, newMember);
    }

    public SetCaseTeamUser(ValueMap json) {
        super(json, CaseTeamUser::deserialize);
    }

    @Override
    protected void process(Team team) {
        team.setUser(newMember);
    }
}
