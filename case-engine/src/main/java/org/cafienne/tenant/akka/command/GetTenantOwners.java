package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantOwnersResponse;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantOwnersRequested;

import java.io.IOException;

@Manifest
public class GetTenantOwners extends TenantCommand {
    public GetTenantOwners(TenantUser tenantOwner, String tenantId) {
        super(tenantOwner, tenantId);
    }

    public GetTenantOwners(ValueMap json) {
        super(json);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        // We add this event to enable some form of audit logging
        tenant.addEvent(new TenantOwnersRequested(tenant)).updateState(tenant);
        return new TenantOwnersResponse(this, tenant.getId(), tenant.getOwners());
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

