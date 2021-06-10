package org.cafienne.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;

@Manifest
public class EnableTenant extends PlatformTenantCommand {
    public EnableTenant(PlatformUser user, String tenantId) {
        super(user, tenantId);
    }

    public EnableTenant(ValueMap json) {
        super(json);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.enable();
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

