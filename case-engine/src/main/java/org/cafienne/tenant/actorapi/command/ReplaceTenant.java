package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.exception.TenantException;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Manifest
public class ReplaceTenant extends TenantCommand {
    private final List<TenantUserInformation> users;

    public ReplaceTenant(TenantUser tenantOwner, List<TenantUserInformation> users) {
        super(tenantOwner);
        this.users = users;
    }

    public ReplaceTenant(ValueMap json) {
        super(json);
        this.users = new ArrayList();
        json.withArray(Fields.users).forEach(user -> this.users.add(TenantUserInformation.from(user.asMap())));
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        // Check whether after the filtering there are still owners left. Tenant must have owners.
        if (users.stream().filter(potentialOwner -> potentialOwner.isOwner() && potentialOwner.isEnabled()).count() == 0) {
            throw new TenantException("Cannot update the tenant and remove all tenant owners or disable their accounts");
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.replaceInstance(users);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        generator.writeArrayFieldStart(Fields.users.toString());
        for (TenantUserInformation user : users) {
            user.writeThisObject(generator);
        }
        generator.writeEndArray();
    }
}

