package org.cafienne.tenant.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantUserEnabled extends TenantEvent {
    public final String userId;

    private enum Fields {
        userId
    }

    public TenantUserEnabled(TenantActor tenant, String userId) {
        super(tenant);
        this.userId = userId;
    }

    public TenantUserEnabled(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
    }

    @Override
    public void updateState(TenantActor tenant) {
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}
