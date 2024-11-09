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

package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.instance.team.Team;

/**
 */
public class MemberAPI extends BaseTeamAPI {
    private final boolean isOwner;

    public MemberAPI(Team team, CaseTeamMember member) {
        super(team);
        this.isOwner = member.isOwner();
        addPropertyReader("isOwner", member::isOwner);
        addPropertyReader("id", member::memberId);
        addPropertyReader("type", member::memberType);
        addPropertyReader("roles", member::getCaseRoles);
    }

    boolean isOwner() {
        return isOwner;
    }
}
