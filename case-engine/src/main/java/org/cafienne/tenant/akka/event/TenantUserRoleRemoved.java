package org.cafienne.tenant.akka.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
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
    protected void updateUserState(User user) {
        user.updateState(this);
    }
}
