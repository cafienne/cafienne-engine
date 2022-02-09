package org.cafienne.consentgroup.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ConsentGroupCreated extends ConsentGroupBaseEvent implements BootstrapMessage {
    public final CafienneVersion engineVersion;
    public final String tenant;

    public ConsentGroupCreated(ConsentGroupActor group, String tenant) {
        super(group);
        this.engineVersion = Cafienne.version();
        this.tenant = tenant;
    }

    public ConsentGroupCreated(ValueMap json) {
        super(json);
        this.tenant = json.readString(Fields.tenant);
        this.engineVersion = json.readObject(Fields.engineVersion, CafienneVersion::new);
    }

    @Override
    public void updateState(ConsentGroupActor group) {
        group.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeConsentGroupEvent(generator);
        writeField(generator, Fields.tenant, this.tenant);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
