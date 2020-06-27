package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

/**
 * Helper class that validates the existence of specified user id in the tenant
 */
abstract class RoleCommand extends ExistingUserCommand {
    public final String role;

    protected enum Fields {
        role
    }

    public RoleCommand(TenantUser tenantOwner, String tenantId, String userId, String role) {
        super(tenantOwner, tenantId, userId);
        this.role = role;
    }

    public RoleCommand(ValueMap json) {
        super(json);
        this.role = readField(json, Fields.role);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, role);
    }
}
