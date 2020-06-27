package org.cafienne.tenant.akka.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

@Manifest
public class OwnerAdded extends TenantUserEvent {
    public OwnerAdded(TenantActor tenant, String userId) {
        super(tenant, userId);
    }

    public OwnerAdded(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUserState(User user) {
        user.updateState(this);
    }
}
