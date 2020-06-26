package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.User;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;

@Manifest
public class AddTenantOwner extends ExistingUserCommand {

    public AddTenantOwner(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId, userId);
    }

    public AddTenantOwner(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUser(User user) {
        // Check if user is already an owner; this command is idempotent
        if (! user.isOwner()) {
            user.makeOwner();
        }
    }
}