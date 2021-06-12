package org.cafienne.tenant.actorapi.event.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantCreated extends PlatformEvent {
    public final CafienneVersion engineVersion;

    public TenantCreated(TenantActor tenant) {
        super(tenant);
        this.engineVersion = Cafienne.version();
    }

    public TenantCreated(ValueMap json) {
        super(json);
        this.engineVersion = new CafienneVersion(readMap(json, Fields.engineVersion));
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}