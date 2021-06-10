package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.response.TenantOwnersResponse;
import org.cafienne.tenant.actorapi.response.TenantResponse;

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

