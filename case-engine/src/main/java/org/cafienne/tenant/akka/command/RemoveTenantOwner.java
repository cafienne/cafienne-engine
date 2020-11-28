package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;
import org.cafienne.tenant.akka.command.exception.TenantException;

@Manifest
public class RemoveTenantOwner extends ExistingUserCommand {

    public RemoveTenantOwner(TenantUser tenantOwner, String userId) {
        super(tenantOwner, userId);
    }

    public RemoveTenantOwner(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (! tenant.isOwner(userId)) {
            throw new TenantException("User '" + userId + "' is not a tenant owner.");
        }
        validateNotLastOwner(tenant, userId);
    }

    @Override
    protected void updateUser(User user) {
        user.removeOwnership();
    }
}
