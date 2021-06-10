package org.cafienne.tenant.actorapi.event;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

@Manifest
public class TenantUserEnabled extends TenantUserEvent {
    public TenantUserEnabled(TenantActor tenant, String userId) {
        super(tenant, userId);
    }

    public TenantUserEnabled(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUserState(User user) {
        user.updateState(this);
    }
}
