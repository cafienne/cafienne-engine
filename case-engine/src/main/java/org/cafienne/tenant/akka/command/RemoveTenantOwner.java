package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

@Manifest
public class RemoveTenantOwner extends ExistingUserCommand {

    public RemoveTenantOwner(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId, userId);
    }

    public RemoveTenantOwner(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (! tenant.isOwner(userId)) {
            throw new InvalidCommandException("User '" + userId + "' is not a tenant owner.");
        }
        if (tenant.getOwnerList().size() == 1) {
            throw new InvalidCommandException("Cannot remove tenant owner. There must be at least one tenant owner.");
        }
    }

    @Override
    protected void updateUser(User user) {
        user.removeOwnership();
    }
}
