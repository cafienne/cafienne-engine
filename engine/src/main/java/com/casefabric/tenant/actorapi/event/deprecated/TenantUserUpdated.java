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

package com.casefabric.tenant.actorapi.event.deprecated;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;
import com.casefabric.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantUserUpdated extends DeprecatedTenantUserEvent {
    public final String name;
    public final String email;

    public TenantUserUpdated(TenantActor tenant, String userId, String name, String email) {
        super(tenant, userId);
        this.name = name;
        this.email = email;
    }

    public TenantUserUpdated(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.email = json.readString(Fields.email);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.email, email);
    }
}
