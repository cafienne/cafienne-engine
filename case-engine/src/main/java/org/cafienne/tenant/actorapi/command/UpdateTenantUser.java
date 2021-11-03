package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

import java.io.IOException;

@Manifest
public class UpdateTenantUser extends ExistingUserCommand {
    private final TenantUserInformation newUser;

    public UpdateTenantUser(TenantUser tenantOwner, String tenant, TenantUserInformation newUser) {
        super(tenantOwner, tenant, newUser.id());
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