package org.cafienne.actormodel.communication.request.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.communication.CaseSystemCommunicationEvent;
import org.cafienne.actormodel.communication.request.state.RemoteActorState;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class ModelActorReplyEvent extends CaseSystemCommunicationEvent {
    public final String targetActorId;

    protected ModelActorReplyEvent(ModelActor actor, String messageId, String targetActorId) {
        super(actor, messageId);
        this.targetActorId = targetActorId;
    }

    protected ModelActorReplyEvent(ValueMap json) {
        super(json);
        this.targetActorId = json.readString(Fields.targetActorId);
    }

    @Override
    public void updateState(ModelActor actor) {
        RemoteActorState<?> state = actor.getRemoteActorState(this.targetActorId);
        state.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeOutgoingRequestEvent(generator);
    }

    public void writeOutgoingRequestEvent(JsonGenerator generator) throws IOException {
        super.writeActorRequestEvent(generator);
        writeField(generator, Fields.targetActorId, targetActorId);
    }
}
