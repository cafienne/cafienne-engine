package org.cafienne.tenant.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantOwnersRequested extends TenantBaseEvent {

    public TenantOwnersRequested(TenantActor tenant) {
        super(tenant);
    }

    public TenantOwnersRequested(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(TenantActor tenant) {
    }
}
