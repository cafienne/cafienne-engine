package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;
import org.cafienne.tenant.akka.command.exception.TenantException;
import org.cafienne.tenant.akka.command.response.TenantResponse;

import java.io.IOException;

/**
 * Helper class that validates the existence of specified user id in the tenant
 */
abstract class ExistingUserCommand extends TenantCommand {
    public final String userId;

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
        if (tenant.getUser(userId) == null) {
            throw new TenantException("User '" + userId + "' doesn't exist in tenant " + tenant.getId());
        }
    }

    @Override
    public ModelResponse process(TenantActor tenant) {
        User user = tenant.getUser(userId);
        updateUser(user);
        return new TenantResponse(this);
    }

    protected abstract void updateUser(User user);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}
