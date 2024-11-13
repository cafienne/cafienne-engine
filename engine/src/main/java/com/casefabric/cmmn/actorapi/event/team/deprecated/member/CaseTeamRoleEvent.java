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

package com.casefabric.cmmn.actorapi.event.team.deprecated.member;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member role events to do initial filtering.
 */
public abstract class CaseTeamRoleEvent extends DeprecatedCaseTeamEvent {
    private final String roleName;

    /**
     * Returns true if the role name is blank
     * @return
     */
    public boolean isMemberItself() {
        return roleName.isBlank();
    }

    protected CaseTeamRoleEvent(ValueMap json) {
        super(json);
        this.roleName = json.readString(Fields.role);
    }

    public String roleName() {
        return roleName;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, roleName);
    }
}
