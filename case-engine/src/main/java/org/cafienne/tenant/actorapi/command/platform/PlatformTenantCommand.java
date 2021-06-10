package org.cafienne.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.exception.AuthorizationException;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.config.Cafienne;
import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.command.TenantCommand;

import java.io.IOException;

/**
 * PlatformTenantCommands can only be executed by platform owners
 */
@Manifest
public abstract class PlatformTenantCommand extends TenantCommand {
    protected PlatformTenantCommand(PlatformUser user, String tenantId) {
        super(TenantUser.fromPlatformOwner(user, tenantId));
    }

    protected PlatformTenantCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(TenantActor modelActor) throws InvalidCommandException {
        if (! Cafienne.isPlatformOwner(getUser())) {
            throw new AuthorizationException("Only platform owners can invoke platform commands");
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

