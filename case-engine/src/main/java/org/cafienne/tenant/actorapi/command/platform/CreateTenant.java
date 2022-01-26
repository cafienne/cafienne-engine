package org.cafienne.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.PlatformOwner;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.exception.TenantException;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;
import java.util.List;

@Manifest
public class CreateTenant extends PlatformTenantCommand implements BootstrapMessage {
    public final String name;
    private final List<TenantUser> users;

    public CreateTenant(PlatformOwner user, String tenantId, String name, List<TenantUser> users) {
        super(user, tenantId);
        this.name = name;
        this.users = users;
        super.validateUserList(users);
    }

    public CreateTenant(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.users = json.readObjects(Fields.users, TenantUser::deserialize);
    }

    @Override
    public String tenant() {
        return name;
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (tenant.exists()) {
            throw new TenantException("Tenant already exists");
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.createInstance(users);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        writeListField(generator, Fields.users, users);
    }
}

