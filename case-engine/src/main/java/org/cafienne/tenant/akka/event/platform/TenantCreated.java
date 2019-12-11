package org.cafienne.tenant.akka.event.platform;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantCreated extends PlatformEvent {

    public TenantCreated(TenantActor tenant) {
        super(tenant);
    }

    public TenantCreated(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.setInitialState(this);
    }
}
