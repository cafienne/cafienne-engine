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

package com.casefabric.cmmn.actorapi.event.team.user;

import com.casefabric.cmmn.actorapi.command.team.CaseTeamUser;
import com.casefabric.cmmn.actorapi.event.team.CaseTeamMemberRemoved;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Event caused when a consent group access to the case is removed.
 */
@Manifest
public class CaseTeamUserRemoved extends CaseTeamMemberRemoved<CaseTeamUser> {
    public CaseTeamUserRemoved(Team team, CaseTeamUser user) {
        super(team, user);
    }

    public CaseTeamUserRemoved(ValueMap json) {
        super(json, CaseTeamUser::deserialize);
    }
}
