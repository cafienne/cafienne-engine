package org.cafienne.tenant.actorapi.event.platform;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantEnabled extends PlatformBaseEvent {

    public TenantEnabled(TenantActor tenant) {
        super(tenant);
    }

    public TenantEnabled(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.updateState(this);
    }
}
