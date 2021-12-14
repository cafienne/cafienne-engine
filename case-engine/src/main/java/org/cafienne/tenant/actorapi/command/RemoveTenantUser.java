package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;

@Manifest
public class RemoveTenantUser extends TenantCommand {
    public final String userId;

    public RemoveTenantUser(TenantUser tenantOwner, String tenant, String userId) {
        super(tenantOwner, tenant);
        this.userId = userId;
    }

    public RemoveTenantUser(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        validateNotLastOwner(tenant, userId);
    }


    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.removeUser(userId);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}