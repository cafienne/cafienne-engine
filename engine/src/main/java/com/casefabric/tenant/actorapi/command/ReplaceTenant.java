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

package com.casefabric.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.identity.TenantUser;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;
import com.casefabric.tenant.TenantActor;

import java.io.IOException;
import java.util.List;

@Manifest
public class ReplaceTenant extends TenantCommand {
    private final List<TenantUser> users;

    public ReplaceTenant(TenantUser tenantOwner, String tenant, List<TenantUser> users) {
        super(tenantOwner, tenant);
        this.users = users;
        super.validateUserList(users);
    }

    public ReplaceTenant(ValueMap json) {
        super(json);
        this.users = json.readObjects(Fields.users, TenantUser::deserialize);
    }

    @Override
    public void processTenantCommand(TenantActor tenant) {
        tenant.replaceInstance(users);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeListField(generator, Fields.users, users);
    }
}

