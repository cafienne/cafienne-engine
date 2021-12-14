package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;

@Manifest
public class SetTenantUser extends TenantCommand {
    public final TenantUser newUser;

    public SetTenantUser(TenantUser tenantOwner, String tenant, TenantUser newUser) {
        super(tenantOwner, tenant);
        this.newUser = newUser;
    }

    public SetTenantUser(ValueMap json) {
        super(json);
        this.newUser = TenantUser.deserialize(json.with(Fields.newTenantUser));
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if ((!newUser.isOwner()) || !newUser.enabled()) {
            validateNotLastOwner(tenant, newUser.id());
        }
    }


    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.setUser(newUser);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.newTenantUser, newUser);
    }
}