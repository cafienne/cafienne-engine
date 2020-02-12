package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.OwnerRemoved;

import java.io.IOException;

@Manifest
public class RemoveTenantOwner extends TenantCommand {
    public final String userId;

    private enum Fields {
        userId
    }

    public RemoveTenantOwner(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId);
        this.userId = userId;
    }

    public RemoveTenantOwner(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (! tenant.isOwner(userId)) {
            throw new InvalidCommandException("User "+userId+" is not a tenant owner.");
        }
        if (tenant.getOwners().size() == 1) {
            throw new InvalidCommandException("Cannot remove tenant owner. There must be at least one tenant owner.");
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new OwnerRemoved(tenant, userId));
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}
