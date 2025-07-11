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

package org.cafienne.engine.cmmn.actorapi.event.team.deprecated.member;

import org.cafienne.engine.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import org.cafienne.engine.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a team member is no longer owner
 */
@Manifest
public class CaseOwnerRemoved extends DeprecatedCaseTeamEvent {
    public CaseOwnerRemoved(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getMemberDescription()+" is no longer owner]";
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
