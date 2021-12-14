package org.cafienne.tenant.actorapi.event.deprecated;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.event.TenantBaseEvent;

import java.io.IOException;

@Manifest
public class TenantOwnersRequested extends TenantBaseEvent {

    public TenantOwnersRequested(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(TenantActor tenant) {
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTenantEvent(generator);
    }
}
