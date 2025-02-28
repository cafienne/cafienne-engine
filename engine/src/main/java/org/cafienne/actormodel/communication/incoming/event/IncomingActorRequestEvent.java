package org.cafienne.actormodel.communication.incoming.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.ModelActorSystemEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class IncomingActorRequestEvent extends ModelActorSystemEvent {
    public final String messageId;
    public final String sourceActorId;

    protected IncomingActorRequestEvent(ModelCommand command, String sourceActorId) {
        super(command.getActor(), command.getMessageId());
        this.messageId = command.getMessageId();
        this.sourceActorId = sourceActorId;
    }

    protected IncomingActorRequestEvent(ValueMap json) {
        super(json);
        this.messageId = json.readString(Fields.messageId);
        this.sourceActorId = json.readString(Fields.targetActorId);
    }

    @Override
    public String getMessageId() {
        return this.messageId;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeActorRequestEvent(generator);
    }

    public void writeActorRequestEvent(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        writeField(generator, Fields.messageId, messageId);
        writeField(generator, Fields.targetActorId, sourceActorId);
    }
}
