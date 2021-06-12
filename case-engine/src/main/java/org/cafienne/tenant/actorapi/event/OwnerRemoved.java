package org.cafienne.tenant.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

@Manifest
public class OwnerRemoved extends TenantUserEvent {
    public OwnerRemoved(TenantActor tenant, String userId) {
        super(tenant, userId);
    }

    public OwnerRemoved(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUserState(User user) {
        user.updateState(this);
    }
}