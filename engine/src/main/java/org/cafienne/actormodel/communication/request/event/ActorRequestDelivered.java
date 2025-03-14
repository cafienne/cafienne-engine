package org.cafienne.actormodel.communication.request.event;

import org.cafienne.actormodel.communication.request.response.ActorRequestDeliveryReceipt;
import org.cafienne.actormodel.communication.request.state.RemoteActorState;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ActorRequestDelivered extends ModelActorReplyEvent {
    public ActorRequestDelivered(RemoteActorState<?> state, ActorRequestDeliveryReceipt receipt) {
        super(state.actor, receipt.getMessageId(), state.targetActorId);
    }

    public ActorRequestDelivered(ValueMap json) {
        super(json);
    }
}
