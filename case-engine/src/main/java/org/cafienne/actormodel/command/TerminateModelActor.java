package org.cafienne.actormodel.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class TerminateModelActor implements CafienneSerializable {
    static {
        // Only register this one when used ;)
        CafienneSerializer.addManifestWrapper(TerminateModelActor.class, TerminateModelActor::new);
    }

    public final String actorId;

    public TerminateModelActor(String actorId) {
        this.actorId = actorId;
    }

    public TerminateModelActor(ValueMap json) {
        this.actorId = json.raw(Fields.actorId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.actorId, actorId);
    }
}

