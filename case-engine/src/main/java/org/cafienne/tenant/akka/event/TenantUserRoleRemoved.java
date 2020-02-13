package org.cafienne.tenant.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;
import java.util.Set;

@Manifest
public class TenantUserRoleRemoved extends TenantEvent {
    public final String userId;
    public final String role;

    private enum Fields {
        userId, role
    }

    public TenantUserRoleRemoved(TenantActor tenant, String userId, String role) {
        super(tenant);
        this.userId = userId;
        this.role = role;
    }

    public TenantUserRoleRemoved(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
        this.role = readField(json, Fields.role);
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.role, role);
    }
}
