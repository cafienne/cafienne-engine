package org.cafienne.actormodel.communication;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.CaseSystemEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class CaseSystemCommunicationEvent extends CaseSystemEvent implements CaseSystemCommunicationMessage {
    protected final String messageId;

    protected CaseSystemCommunicationEvent(ModelActor actor, String messageId) {
        super(actor);
        this.messageId = messageId;
    }

    protected CaseSystemCommunicationEvent(ValueMap json) {
        super(json);
        this.messageId = json.readString(Fields.messageId);
    }

    @Override
    public void updateState(ModelActor actor) {
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
    }
}
