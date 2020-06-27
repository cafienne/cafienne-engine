package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;
import org.cafienne.tenant.akka.command.response.TenantResponse;

@Manifest
public class RemoveTenantUserRole extends RoleCommand {
    public RemoveTenantUserRole(TenantUser tenantOwner, String tenantId, String userId, String role) {
        super(tenantOwner, tenantId, userId, role);
    }

    public RemoveTenantUserRole(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUser(User user) {
        user.removeRole(role);
    }
}