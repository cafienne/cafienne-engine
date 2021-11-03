package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Helper class that validates the existence of specified user id in the tenant
 */
abstract class RoleCommand extends ExistingUserCommand {
    public final String role;

    protected RoleCommand(TenantUser tenantOwner, String tenant, String userId, String role) {
        super(tenantOwner, tenant, userId);
        this.role = role;
    }

    protected RoleCommand(ValueMap json) {
        super(json);
        this.role = json.readString(Fields.role);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, role);
    }
}
