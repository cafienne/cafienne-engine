package org.cafienne.tenant.actorapi.command;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.User;

@Manifest
public class RemoveTenantUserRole extends RoleCommand {
    public RemoveTenantUserRole(TenantUser tenantOwner, String userId, String role) {
        super(tenantOwner, userId, role);
    }

    public RemoveTenantUserRole(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUser(User user) {
        user.removeRole(role);
    }
}