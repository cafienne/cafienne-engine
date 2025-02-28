package org.cafienne.actormodel.communication.outgoing.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.communication.ModelActorSystemEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class OutgoingActorRequestEvent extends ModelActorSystemEvent {
    public final String targetActorId;

    protected OutgoingActorRequestEvent(ModelActor actor, String messageId, String targetActorId) {
        super(actor, messageId);
        this.targetActorId = targetActorId;
    }

    protected OutgoingActorRequestEvent(ValueMap json) {
        super(json);
        this.targetActorId = json.readString(Fields.targetActorId);
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
