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

package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class SetTenantUser extends TenantCommand {
    public final TenantUser newUser;

    public SetTenantUser(TenantUser tenantOwner, String tenant, TenantUser newUser, String rootCaseId) {
        super(tenantOwner, tenant, rootCaseId);
        this.newUser = newUser;
    }

    public SetTenantUser(ValueMap json) {
        super(json);
        this.newUser = TenantUser.deserialize(json.with(Fields.newTenantUser));
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if ((!newUser.isOwner()) || !newUser.enabled()) {
            validateNotLastOwner(tenant, newUser.id());
        }
    }


    @Override
    public void processTenantCommand(TenantActor tenant) {
        tenant.setUser(newUser);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.newTenantUser, newUser);
    }
}