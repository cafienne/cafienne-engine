package org.cafienne.actormodel.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class TerminateModelActor extends BaseModelCommand<ModelActor, UserIdentity> {
    static {
        // Only register this one when used ;)
        CafienneSerializer.addManifestWrapper(TerminateModelActor.class, TerminateModelActor::new);
    }

    public final String actorId;

    public TerminateModelActor(UserIdentity user, String actorId) {
        super(user, actorId);
        this.actorId = actorId;
    }

    public TerminateModelActor(ValueMap json) {
        super(json);
        this.actorId = json.readString(Fields.actorId);
    }

    @Override
    public UserIdentity readUser(ValueMap json) {
        return null;
    }

    @Override
    public Class<ModelActor> actorClass() {
        return ModelActor.class;
    }

    @Override
    public void validate(ModelActor modelActor) throws InvalidCommandException {

    }

    @Override
    public ModelResponse process(ModelActor modelActor) {
        return null;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.actorId, actorId);
    }
}

