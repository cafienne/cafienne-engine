package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantUserRoleAdded;

import java.io.IOException;

@Manifest
public class AddTenantUserRoles extends TenantCommand {
    public final String userId;
    public final String role;

    private enum Fields {
        userId, role
    }

    public AddTenantUserRoles(TenantUser tenantOwner, String tenantId, String userId, String role) {
        super(tenantOwner, tenantId);
        this.userId = userId;
        this.role = role;
    }

    public AddTenantUserRoles(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
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
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.role, role);
    }
}