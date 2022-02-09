package org.cafienne.tenant.actorapi.event.user;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantUserRemoved extends TenantMemberEvent {

    public TenantUserRemoved(TenantActor tenant, TenantUser user) {
        super(tenant, user);
    }

    public TenantUserRemoved(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(TenantActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTenantUserEvent(generator);
    }
}
