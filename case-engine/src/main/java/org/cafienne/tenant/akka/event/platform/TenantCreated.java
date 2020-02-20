package org.cafienne.tenant.akka.event.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CafienneVersion;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.event.CaseDefinitionApplied;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantCreated extends PlatformEvent {
    public final CafienneVersion engineVersion;

    public enum Fields {
        createdOn, createdBy, parentActorId, rootActorId, caseName, definition, engineVersion
    }

    public TenantCreated(TenantActor tenant) {
        super(tenant);
        this.engineVersion = CaseSystem.version();
    }

    public TenantCreated(ValueMap json) {
        super(json);
        this.engineVersion = new CafienneVersion(readMap(json, Fields.engineVersion));
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.setInitialState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
