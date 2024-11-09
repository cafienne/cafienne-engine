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

package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in team events to do initial filtering.
 */
public abstract class CaseTeamEvent extends CaseBaseEvent {
    protected CaseTeamEvent(Team team) {
        super(team.getCaseInstance());
    }

    protected CaseTeamEvent(ValueMap json) {
        super(json);
    }

    protected void writeCaseTeamEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
    }

    @Override
    public void updateState(Case actor) {
        updateState(actor.getCaseTeam());
    }

    protected abstract void updateState(Team team);
}
