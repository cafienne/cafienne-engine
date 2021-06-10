package org.cafienne.tenant.actorapi.command;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.User;

@Manifest
public class AddTenantUserRole extends RoleCommand {

    public AddTenantUserRole(TenantUser tenantOwner, String userId, String role) {
        super(tenantOwner, userId, role);
    }

    public AddTenantUserRole(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUser(User user) {
        user.addRole(role);
    }
}