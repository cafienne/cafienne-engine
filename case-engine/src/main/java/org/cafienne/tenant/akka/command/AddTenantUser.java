package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.platform.TenantEnabled;

import java.io.IOException;
import java.util.Set;

@Manifest
public class AddTenantUser extends TenantCommand {
    private final TenantUser newUser;

    private enum Fields {
        newTenantUser
    }

    public AddTenantUser(TenantUser tenantOwner, String tenantId, TenantUser newUser) {
        super(tenantOwner, tenantId);
        this.newUser = newUser;
    }

    public AddTenantUser(ValueMap json) {
        super(json);
        this.newUser = TenantUser.from(json.with(Fields.newTenantUser));
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (tenant.getUser(newUser.id()) != null) {
            throw new InvalidCommandException("User '" + newUser.id() + "' already exists in tenant " + tenant.getId());
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.createUser(newUser);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.newTenantUser, newUser.toValue());
    }
}