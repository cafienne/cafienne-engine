package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;

import java.io.IOException;

@Manifest
public class UpsertTenantUser extends TenantCommand {
    private final TenantUser newUser;

    public UpsertTenantUser(TenantUser tenantOwner, String tenantId, TenantUser newUser) {
        super(tenantOwner, tenantId);
        this.newUser = newUser;
    }

    public UpsertTenantUser(ValueMap json) {
        super(json);
        this.newUser = TenantUser.from(json.with(Fields.newTenantUser));
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.upsertUser(newUser);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.newTenantUser, newUser.toValue());
    }
}