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

/**
 * Helper class that validates the existence of specified user id in the tenant
 */
abstract class ExistingUserCommand extends TenantCommand {
    public final String userId;

    private enum Fields {
        userId
    }

    public ExistingUserCommand(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId);
        this.userId = userId;
    }

    public ExistingUserCommand(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (! tenant.isUser(userId)) {
            throw new InvalidCommandException("User '" + userId + "' doesn't exist in tenant " + tenant.getId());
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}
