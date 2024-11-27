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

package org.cafienne.tenant.actorapi.event.deprecated;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.event.TenantBaseEvent;

import java.io.IOException;

/**
 * TenantUserEvents are generated on tenant users.
 */
public abstract class DeprecatedTenantUserEvent extends TenantBaseEvent {
    public final String userId;

    protected DeprecatedTenantUserEvent(TenantActor tenant, String userId) {
        super(tenant);
        this.userId = userId;
    }

    protected DeprecatedTenantUserEvent(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
    }

    @Override
    public void updateState(TenantActor actor) {
        actor.updateState(this);
    }

    @Override
    public String getDescription() {
        return super.getDescription() +" on user " + userId;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTenantEvent(generator);
        writeField(generator, Fields.userId, userId);
    }
}
