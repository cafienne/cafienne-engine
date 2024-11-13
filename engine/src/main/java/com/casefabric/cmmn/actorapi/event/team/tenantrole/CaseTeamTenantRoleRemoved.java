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

package com.casefabric.cmmn.actorapi.event.team.tenantrole;

import com.casefabric.cmmn.actorapi.command.team.CaseTeamTenantRole;
import com.casefabric.cmmn.actorapi.event.team.CaseTeamMemberRemoved;
import com.casefabric.cmmn.instance.team.Team;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamTenantRoleRemoved extends CaseTeamMemberRemoved<CaseTeamTenantRole> {
    public CaseTeamTenantRoleRemoved(Team team, CaseTeamTenantRole caseTeamTenantRole) {
        super(team, caseTeamTenantRole);
    }

    public CaseTeamTenantRoleRemoved(ValueMap json) {
        super(json, CaseTeamTenantRole::deserialize);
    }
}