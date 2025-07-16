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

package org.cafienne.userregistration.tenant.actorapi.event.deprecated;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.userregistration.tenant.TenantActor;

import java.io.IOException;

/**
 * TenantUserRoleEvents are generated on role changes of tenant users
 */
public abstract class TenantUserRoleEvent extends DeprecatedTenantUserEvent {

    public final String role;

    protected TenantUserRoleEvent(TenantActor tenant, String userId, String role) {
        super(tenant, userId);
        this.role = role;
    }

    protected TenantUserRoleEvent(ValueMap json) {
        super(json);
        this.role = json.readString(Fields.role);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, role);
    }
}
