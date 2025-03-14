package org.cafienne.actormodel.communication.request.response;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.request.state.RemoteActorState;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ActorRequestDeliveryReceipt extends CaseSystemCommunicationResponse {
    public ActorRequestDeliveryReceipt(ModelCommand command) {
        super(command);
    }

    public ActorRequestDeliveryReceipt(ValueMap json) {
        super(json);
    }

    @Override
    public String getCommandDescription() {
        return "Delivered[" + command.getDescription() + "]";
    }

    @Override
    protected void process(RemoteActorState<?> state) {
        state.registerDelivery(this);
    }
}
