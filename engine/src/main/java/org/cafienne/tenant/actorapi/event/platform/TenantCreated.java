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

package org.cafienne.tenant.actorapi.event.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.infrastructure.EngineVersion;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantCreated extends PlatformBaseEvent implements BootstrapMessage {
    public final EngineVersion engineVersion;

    public TenantCreated(TenantActor tenant) {
        super(tenant);
        this.engineVersion = tenant.caseSystem.version();
    }

    public TenantCreated(ValueMap json) {
        super(json);
        this.engineVersion = json.readObject(Fields.engineVersion, EngineVersion::new);
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
