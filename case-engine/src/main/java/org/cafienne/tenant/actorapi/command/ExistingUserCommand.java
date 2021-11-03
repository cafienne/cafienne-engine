package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;
import org.cafienne.tenant.actorapi.exception.TenantException;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;

/**
 * Helper class that validates the existence of specified user id in the tenant
 */
abstract class ExistingUserCommand extends TenantCommand {
    public final String userId;

    protected ExistingUserCommand(TenantUser tenantOwner, String tenant, String userId) {
        super(tenantOwner, tenant);
        this.userId = userId;
    }

    protected ExistingUserCommand(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (tenant.getUser(userId) == null) {
            throw new TenantException("User '" + userId + "' doesn't exist in tenant " + tenant.getId());
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
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
