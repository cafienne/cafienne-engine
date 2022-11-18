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

package org.cafienne.tenant.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.actorapi.command.GetTenantOwners;

import java.io.IOException;
import java.util.List;

@Manifest
public class TenantOwnersResponse extends TenantResponse {
    public final String name;
    public final List<String> owners;

    public TenantOwnersResponse(GetTenantOwners command, String name, List<String> owners) {
        super(command);
        this.name = name;
        this.owners = owners;
    }

    public TenantOwnersResponse(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.owners = json.withArray(Fields.owners).rawList();
    }

    @Override
    public Value<?> toJson() {
        return Value.convert(owners);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.owners, owners);
    }
}
