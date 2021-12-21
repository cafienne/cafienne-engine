package org.cafienne.tenant.actorapi.event.user;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantUserAdded extends TenantMemberEvent {

    public TenantUserAdded(TenantActor tenant, TenantUser user) {
        super(tenant, user);
    }

    public TenantUserAdded(ValueMap json) {
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
