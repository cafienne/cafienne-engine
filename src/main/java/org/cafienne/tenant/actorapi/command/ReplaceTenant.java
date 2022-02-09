package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;
import java.util.List;

@Manifest
public class ReplaceTenant extends TenantCommand {
    private final List<TenantUser> users;

    public ReplaceTenant(TenantUser tenantOwner, String tenant, List<TenantUser> users) {
        super(tenantOwner, tenant);
        this.users = users;
        super.validateUserList(users);
    }

    public ReplaceTenant(ValueMap json) {
        super(json);
        this.users = json.readObjects(Fields.users, TenantUser::deserialize);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.replaceInstance(users);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeListField(generator, Fields.users, users);
    }
}

