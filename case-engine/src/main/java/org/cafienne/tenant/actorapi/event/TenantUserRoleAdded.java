package org.cafienne.tenant.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

@Manifest
public class TenantUserRoleAdded extends TenantUserRoleEvent {
    public TenantUserRoleAdded(TenantActor tenant, String userId, String role) {
        super(tenant, userId, role);
    }

    public TenantUserRoleAdded(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return super.getDescription() +" - added role " + role;
    }

    @Override
    protected void updateUserState(User user) {
        user.updateState(this);
    }
}
