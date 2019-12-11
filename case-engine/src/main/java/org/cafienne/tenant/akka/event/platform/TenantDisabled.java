package org.cafienne.tenant.akka.event.platform;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.event.TenantEvent;

@Manifest
public class TenantDisabled extends PlatformEvent {

    public TenantDisabled(TenantActor tenant) {
        super(tenant);
    }

    public TenantDisabled(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.disable(this);
    }
}
