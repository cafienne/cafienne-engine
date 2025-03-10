package org.cafienne.actormodel.communication.outgoing.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.outgoing.RemoteActorState;
import org.cafienne.actormodel.communication.outgoing.command.RequestModelActor;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ActorRequestCreated extends OutgoingActorRequestEvent {
    public final ModelCommand command;

    public ActorRequestCreated(RemoteActorState<?> state, ModelCommand command) {
        super(state.actor, command.getMessageId(), state.targetActorId);
        this.command = command;
    }

    public ActorRequestCreated(ValueMap json) {
        super(json);
        this.command = json.readModelCommand(Fields.command);
    }

    @Override
    public void updateState(ModelActor actor) {
        RemoteActorState<?> state = actor.getRemoteActorState(this.command.getActorId());
        state.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeOutgoingRequestEvent(generator);
        writeField(generator, Fields.command, command);
    }
}
