package org.cafienne.actormodel.communication.outgoing.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.communication.ModelActorSystemEvent;
import org.cafienne.actormodel.communication.outgoing.response.ActorRequestDeliveryReceipt;
import org.cafienne.actormodel.communication.outgoing.RemoteActorState;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ActorRequestDelivered extends ModelActorSystemEvent {
    protected final String targetActorId;

    public ActorRequestDelivered(RemoteActorState<?> state, ActorRequestDeliveryReceipt receipt) {
        super(state.actor, receipt.getMessageId());
        this.targetActorId = state.targetActorId;
    }

    public ActorRequestDelivered(ValueMap json) {
        super(json);
        this.targetActorId = json.readString(Fields.targetActorId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeActorRequestEvent(generator);
        writeField(generator, Fields.targetActorId, targetActorId);
    }
}
