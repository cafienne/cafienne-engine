package org.cafienne.tenant.akka.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.platform.TenantDisabled;

import java.io.IOException;

@Manifest
public class DisableTenant extends PlatformTenantCommand {
    public DisableTenant(PlatformUser user, String tenantId) {
        super(user, tenantId);
    }

    public DisableTenant(ValueMap json) {
        super(json);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new TenantDisabled(tenant)).updateState(tenant);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

