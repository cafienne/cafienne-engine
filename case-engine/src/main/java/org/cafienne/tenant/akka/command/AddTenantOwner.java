package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.OwnerAdded;

@Manifest
public class AddTenantOwner extends ExistingUserCommand {

    public AddTenantOwner(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId, userId);
    }

    public AddTenantOwner(ValueMap json) {
        super(json);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        // Check if user is already an owner
        if (! tenant.isOwner(userId)) {
            tenant.addEvent(new OwnerAdded(tenant, userId));
        }
        return new TenantResponse(this);
    }
}