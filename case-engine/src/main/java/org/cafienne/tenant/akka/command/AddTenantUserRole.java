package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantUserRoleAdded;

import java.io.IOException;

@Manifest
public class AddTenantUserRole extends ExistingUserCommand {
    public final String role;

    private enum Fields {
        role
    }

    public AddTenantUserRole(TenantUser tenantOwner, String tenantId, String userId, String role) {
        super(tenantOwner, tenantId, userId);
        this.role = role;
    }

    public AddTenantUserRole(ValueMap json) {
        super(json);
        this.role = readField(json, Fields.role);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new TenantUserRoleAdded(tenant, userId, role));
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, role);
    }
}