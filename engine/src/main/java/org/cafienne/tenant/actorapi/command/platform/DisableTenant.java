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

package org.cafienne.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.PlatformOwner;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class DisableTenant extends PlatformTenantCommand {
    public DisableTenant(PlatformOwner user, String tenantId, String rootCaseId) {
        super(user, tenantId, rootCaseId);
    }

    public DisableTenant(ValueMap json) {
        super(json);
    }

    @Override
    public void processTenantCommand(TenantActor tenant) {
        tenant.disable();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

