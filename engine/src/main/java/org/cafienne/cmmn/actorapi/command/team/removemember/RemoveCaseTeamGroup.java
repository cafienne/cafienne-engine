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

package org.cafienne.cmmn.actorapi.command.team.removemember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamGroup;
import org.cafienne.cmmn.instance.team.MemberType;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to remove a consent group from the case team
 */
@Manifest
public class RemoveCaseTeamGroup extends RemoveCaseTeamMemberCommand<CaseTeamGroup> {
    public RemoveCaseTeamGroup(CaseUserIdentity user, String caseInstanceId, String rootCaseId, String groupId) {
        super(user, caseInstanceId, rootCaseId, groupId);
    }

    public RemoveCaseTeamGroup(ValueMap json) {
        super(json);
    }

    @Override
    protected MemberType type() {
        return MemberType.TenantRole;
    }

    @Override
    protected CaseTeamGroup member(Team team) {
        return team.getGroup(memberId);
    }

    @Override
    protected void process(Team team) {
        team.removeGroup(memberId);
    }
}
