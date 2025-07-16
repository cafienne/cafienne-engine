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

package org.cafienne.userregistration.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.PlatformOwner;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.userregistration.tenant.TenantActor;
import org.cafienne.userregistration.tenant.actorapi.command.TenantCommand;

import java.io.IOException;

/**
 * PlatformTenantCommands can only be executed by platform owners
 */
@Manifest
public abstract class PlatformTenantCommand extends TenantCommand {
    protected PlatformTenantCommand(PlatformOwner user, String tenantId) {
        super(user.asTenantUser(tenantId), tenantId);
    }

    protected PlatformTenantCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(TenantActor modelActor) throws InvalidCommandException {
        if (! modelActor.caseSystem.config().platform().isPlatformOwner(getUser())) {
            throw new AuthorizationException("Only platform owners can invoke platform commands");
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

