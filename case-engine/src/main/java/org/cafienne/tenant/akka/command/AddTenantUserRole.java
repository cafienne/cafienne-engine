package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;
import org.cafienne.tenant.akka.command.response.TenantResponse;

@Manifest
public class AddTenantUserRole extends RoleCommand {

    public AddTenantUserRole(TenantUser tenantOwner, String tenantId, String userId, String role) {
        super(tenantOwner, tenantId, userId, role);
    }

    public AddTenantUserRole(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUser(User user) {
        user.addRole(role);
    }
}