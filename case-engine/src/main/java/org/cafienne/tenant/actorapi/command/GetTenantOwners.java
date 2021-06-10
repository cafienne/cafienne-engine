package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.command.response.TenantOwnersResponse;
import org.cafienne.tenant.actorapi.command.response.TenantResponse;

import java.io.IOException;

@Manifest
public class GetTenantOwners extends TenantCommand {
    public GetTenantOwners(TenantUser tenantOwner) {
        super(tenantOwner);
    }

    public GetTenantOwners(ValueMap json) {
        super(json);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        return new TenantOwnersResponse(this, tenant.getId(), tenant.getOwnerList());
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

