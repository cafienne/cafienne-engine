package org.cafienne.tenant.actorapi.event.deprecated;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

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
}
