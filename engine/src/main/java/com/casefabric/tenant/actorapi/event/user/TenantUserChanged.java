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

package com.casefabric.tenant.actorapi.event.user;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.identity.TenantUser;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;
import com.casefabric.tenant.TenantActor;

import java.io.IOException;
import java.util.Set;

@Manifest
public class TenantUserChanged extends TenantMemberEvent {
    public final Set<String> rolesRemoved;

    public TenantUserChanged(TenantActor tenant, TenantUser user, Set<String> rolesRemoved) {
        super(tenant, user);
        this.rolesRemoved = rolesRemoved;
    }

    public TenantUserChanged(ValueMap json) {
        super(json);
        this.rolesRemoved = json.readSet(Fields.rolesRemoved);
    }

    @Override
    public void updateState(TenantActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTenantUserEvent(generator);
        writeField(generator, Fields.rolesRemoved, rolesRemoved);
    }
}
