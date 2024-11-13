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

package com.casefabric.cmmn.actorapi.event.team.deprecated.user;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TeamMemberAdded and TeamMemberRemoved are no longer generated
 */
public abstract class DeprecatedCaseTeamUserEvent extends DeprecatedCaseTeamEvent {
    protected final String userId;
    protected final Set<String> roles;

    protected DeprecatedCaseTeamUserEvent(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
        this.roles = json.readSet(Fields.roles);
    }

    @Override
    public boolean isUserEvent() {
        return true;
    }

    @Override
    public String getId() {
        return userId;
    }

    /**
     * Name/id of user that is added or removed. Isolating logic in a single place
     * @return
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Roles the member had.
     */
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public String getDescription() {
        if (roles.size() == 1) {
            return getClass().getSimpleName() + "['" + userId + "' left team]";
        } else {
            String rolesString = (roles.stream().filter(role -> !role.isBlank()).map(role -> "'" + role + "'").collect(Collectors.joining(", ")));
            return getClass().getSimpleName() + "['" +userId + "' with roles " + rolesString + " left the team]";
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.roles, roles);
    }
}
