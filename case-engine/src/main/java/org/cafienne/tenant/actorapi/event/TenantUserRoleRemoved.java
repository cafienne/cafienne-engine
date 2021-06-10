package org.cafienne.tenant.actorapi.event;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

@Manifest
public class TenantUserRoleRemoved extends TenantUserRoleEvent {

    public TenantUserRoleRemoved(TenantActor tenant, String userId, String role) {
        super(tenant, userId, role);
    }

    public TenantUserRoleRemoved(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return super.getDescription() +" - removed role " + role;
    }

    @Override
    protected void updateUserState(User user) {
        user.updateState(this);
    }
}
