package org.cafienne.tenant.actorapi.event.platform;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantEnabled extends PlatformEvent {

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
