package org.cafienne.actormodel.communication.incoming.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ActorRequestExecuted extends IncomingActorRequestEvent {
    private final ModelCommand command;

    public ActorRequestExecuted(ModelCommand command, String sourceActorId) {
        super(command, sourceActorId);
        this.command = command;
    }

    public ActorRequestExecuted(ValueMap json) {
        super(json);
        String manifest = json.readString(Fields.manifest);
        this.command = (ModelCommand) new CafienneSerializer().fromJson(json.readMap(Fields.content), manifest);
    }

    @Override
    public String getMessageId() {
        return command.getMessageId();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        generator.writeStringField(Fields.manifest.toString(), CafienneSerializer.getManifestString(command));
        generator.writeFieldName(Fields.content.toString());
        command.writeThisObject(generator);
    }
}
