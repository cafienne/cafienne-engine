package org.cafienne.tenant.akka.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantOwnersRequested extends TenantEvent {

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
