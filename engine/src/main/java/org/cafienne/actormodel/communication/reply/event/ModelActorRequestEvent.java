package org.cafienne.actormodel.communication.reply.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.CaseSystemCommunicationEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class ModelActorRequestEvent extends CaseSystemCommunicationEvent {
    public final String sourceActorId;

    protected ModelActorRequestEvent(ModelCommand command, String sourceActorId) {
        super(command.getActor(), command.getMessageId());
        this.sourceActorId = sourceActorId;
    }

    protected ModelActorRequestEvent(ValueMap json) {
        super(json);
        this.sourceActorId = json.readString(Fields.sourceActorId);
    }

    @Override
    public void updateState(ModelActor actor) {
        actor.getIncomingRequestState().updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeIncomingRequestEvent(generator);
    }

    public void writeIncomingRequestEvent(JsonGenerator generator) throws IOException {
        super.writeActorRequestEvent(generator);
        writeField(generator, Fields.sourceActorId, sourceActorId);
    }
}
