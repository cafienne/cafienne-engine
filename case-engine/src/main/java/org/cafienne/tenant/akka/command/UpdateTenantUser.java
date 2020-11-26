package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

import java.io.IOException;

@Manifest
public class UpdateTenantUser extends ExistingUserCommand {
    private final TenantUserInformation newUser;

    public UpdateTenantUser(TenantUser tenantOwner, TenantUserInformation newUser) {
        super(tenantOwner, newUser.id());
        this.newUser = newUser;
    }

    public UpdateTenantUser(ValueMap json) {
        super(json);
        this.newUser = TenantUserInformation.from(json.with(Fields.newTenantUser));
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        validateNotLastOwner(tenant, newUser);
    }

    @Override
    protected void updateUser(User user) {
        user.upsertWith(newUser);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.newTenantUser, newUser.toValue());
    }
}