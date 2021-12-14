package org.cafienne.tenant.actorapi.event.deprecated;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

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
}
