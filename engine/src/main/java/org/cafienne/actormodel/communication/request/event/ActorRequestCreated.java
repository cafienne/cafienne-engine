package org.cafienne.actormodel.communication.request.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.request.state.RemoteActorState;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ActorRequestCreated extends ModelActorReplyEvent {
    public final ModelCommand command;

    public ActorRequestCreated(RemoteActorState<?> state, ModelCommand command) {
        super(state.actor, command.getMessageId(), state.targetActorId);
        this.command = command;
    }

    public ActorRequestCreated(ValueMap json) {
        super(json);
        this.command = json.readManifestField(Fields.command);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeOutgoingRequestEvent(generator);
        writeManifestField(generator, Fields.command, command);
    }
}
