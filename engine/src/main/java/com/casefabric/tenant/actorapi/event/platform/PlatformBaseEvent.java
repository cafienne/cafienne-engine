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

package com.casefabric.tenant.actorapi.event.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.json.ValueMap;
import com.casefabric.tenant.TenantActor;
import com.casefabric.tenant.actorapi.event.TenantBaseEvent;

import java.io.IOException;

/**
 * Platform events are generated by platform owners that administer the tenants in {@link TenantActor}.
 */
public abstract class PlatformBaseEvent extends TenantBaseEvent implements PlatformEvent {
    protected PlatformBaseEvent(TenantActor tenant) {
        super(tenant);
    }

    protected PlatformBaseEvent(ValueMap json) {
        super(json);
    }

    public String tenantName() {
        return getActorId();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTenantEvent(generator);
    }
}
