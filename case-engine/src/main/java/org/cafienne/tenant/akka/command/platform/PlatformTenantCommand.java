package org.cafienne.tenant.akka.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.TenantCommand;

import java.io.IOException;

/**
 * PlatformTenantCommands can only be executed by platform owners
 */
@Manifest
abstract class PlatformTenantCommand extends TenantCommand {
    protected PlatformTenantCommand(PlatformUser user, String tenantId) {
        super(TenantUser.fromPlatformOwner(user, tenantId), tenantId);
    }

    protected PlatformTenantCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(TenantActor modelActor) throws InvalidCommandException {
        if (modelActor.exists()) {
//            System.err.println("Tenant "+modelActor.getId()+ " already exixsts");
            throw new InvalidCommandException("Tenant already exists");
        }

        if (! CaseSystem.isPlatformOwner(getUser())) {
            throw new InvalidCommandException("You do not have the privileges to create a tenant");
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

