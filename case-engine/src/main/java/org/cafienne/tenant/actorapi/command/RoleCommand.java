package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.ValueMap;

import java.io.IOException;

/**
 * Helper class that validates the existence of specified user id in the tenant
 */
abstract class RoleCommand extends ExistingUserCommand {
    public final String role;

    public RoleCommand(TenantUser tenantOwner, String userId, String role) {
        super(tenantOwner, userId);
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
