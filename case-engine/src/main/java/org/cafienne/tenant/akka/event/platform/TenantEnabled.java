package org.cafienne.tenant.akka.event.platform;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
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
