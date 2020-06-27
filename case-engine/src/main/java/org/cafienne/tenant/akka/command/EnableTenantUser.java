package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantUserEnabled;

@Manifest
public class EnableTenantUser extends ExistingUserCommand {

    public EnableTenantUser(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId, userId);
    }

    public EnableTenantUser(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUser(User user) {
        user.enable();
    }
}