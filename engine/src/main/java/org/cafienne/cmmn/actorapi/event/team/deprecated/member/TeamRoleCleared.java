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

package org.cafienne.cmmn.actorapi.event.team.deprecated.member;

import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a role is removed from a case team member
 */
@Manifest
public class TeamRoleCleared extends CaseTeamRoleEvent {
    public TeamRoleCleared(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        if (isMemberItself()) {
            // The event that removes the member to the team; in practice this one cannot be invoked
            //  since that can only be done through removing the member explicitly, resulting in TeamMemberRemoved event
            return getClass().getSimpleName() + "[" + getMemberDescription()+" is removed from the case team]";
        } else {
            return getClass().getSimpleName() + "[" + getMemberDescription()+" no longer has role " + roleName() + " in the case team]";
        }
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
