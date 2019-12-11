package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantUserDisabled;

import java.io.IOException;

@Manifest
public class DisableTenantUser extends TenantCommand {
    public final String userId;

    private enum Fields {
        userId
    }

    public DisableTenantUser(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId);
        this.userId = userId;
    }

    public DisableTenantUser(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new TenantUserDisabled(tenant, userId)).updateState(tenant);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}