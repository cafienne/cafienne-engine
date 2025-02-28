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
public class ActorRequestDelivered extends OutgoingActorRequestEvent {
    public ActorRequestDelivered(RemoteActorState<?> state, ActorRequestDeliveryReceipt receipt) {
        super(state.actor, receipt.getMessageId(), state.targetActorId);
    }

    public ActorRequestDelivered(ValueMap json) {
        super(json);
    }
}
