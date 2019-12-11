package org.cafienne.tenant.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantUserCreated extends TenantEvent {
    public final String userId;
    public final String name;
    public final String email;

    private enum Fields {
        userId, name, email
    }

    public TenantUserCreated(TenantActor tenant, String userId, String name, String email) {
        super(tenant);
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public TenantUserCreated(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
        this.name = readField(json, Fields.name);
        this.email = readField(json, Fields.email);
    }

    @Override
    public void updateState(TenantActor tenant) {
        // No state to update. Simply pass the event
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.email, email);
    }
}
