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

package com.casefabric.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.command.BootstrapMessage;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.PlatformOwner;
import com.casefabric.actormodel.identity.TenantUser;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;
import com.casefabric.tenant.TenantActor;
import com.casefabric.tenant.actorapi.exception.TenantException;

import java.io.IOException;
import java.util.List;

@Manifest
public class CreateTenant extends PlatformTenantCommand implements BootstrapMessage {
    public final String name;
    private final List<TenantUser> users;

    public CreateTenant(PlatformOwner user, String tenantId, String name, List<TenantUser> users) {
        super(user, tenantId);
        this.name = name;
        this.users = users;
        super.validateUserList(users);
    }

    public CreateTenant(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.users = json.readObjects(Fields.users, TenantUser::deserialize);
    }

    @Override
    public String tenant() {
        return name;
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (tenant.exists()) {
            throw new TenantException("Tenant already exists");
        }
    }

    @Override
    public void processTenantCommand(TenantActor tenant) {
        tenant.createInstance(users);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        writeListField(generator, Fields.users, users);
    }
}

