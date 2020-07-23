package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.User;

@Manifest
public class DisableTenantUser extends ExistingUserCommand {

    public DisableTenantUser(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId, userId);
    }

    public DisableTenantUser(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUser(User user) {
        user.disable();
    }
}