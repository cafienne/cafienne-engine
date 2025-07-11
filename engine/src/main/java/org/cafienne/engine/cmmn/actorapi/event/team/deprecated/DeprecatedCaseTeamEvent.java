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

package org.cafienne.engine.cmmn.actorapi.event.team.deprecated;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.actorapi.event.team.CaseTeamEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * TeamMemberAdded and TeamMemberRemoved are no longer generated
 */
public abstract class DeprecatedCaseTeamEvent extends CaseTeamEvent {
    public final String memberId;
    public final boolean isTenantUser;

    protected DeprecatedCaseTeamEvent(ValueMap json) {
        super(json);
        this.memberId = json.readString(Fields.memberId);
        this.isTenantUser = json.readBoolean(Fields.isTenantUser, true);
    }

    public boolean isUserEvent() {
        return isTenantUser;
    }

    public String getId() {
        return memberId;
    }

    public String roleName() {
        return "";
    }

    protected String getMemberDescription() {
        return "Tenant " + (isTenantUser ? "user " : "role ") + getId();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.memberId, memberId);
        writeField(generator, Fields.isTenantUser, isTenantUser);
    }

}
