package org.cafienne.actormodel.communication.incoming.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.ModelActorSystemEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class IncomingActorRequestEvent extends ModelActorSystemEvent {
    public final String sourceActorId;

    protected IncomingActorRequestEvent(ModelCommand command, String sourceActorId) {
        super(command.getActor(), command.getMessageId());
        this.sourceActorId = sourceActorId;
    }

    protected IncomingActorRequestEvent(ValueMap json) {
        super(json);
        this.sourceActorId = json.readString(Fields.sourceActorId);
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
