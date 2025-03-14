package org.cafienne.actormodel.communication.reply.state;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.communication.reply.event.ActorRequestStored;
import org.cafienne.actormodel.communication.reply.event.ModelActorRequestEvent;
import org.cafienne.actormodel.communication.request.command.RequestModelActor;

import java.util.HashMap;
import java.util.Map;

public class IncomingRequestState {
    private final Map<String, IncomingRequest> requests = new HashMap<>();
    final ModelActor actor;

    public IncomingRequestState(ModelActor actor) {
        this.actor = actor;
    }

    public void handleIncomingRequest(RequestModelActor request) {
//        System.out.println("\n" + actor +"   RECEIVED REQUEST " + request.getCommandDescription() +" with id " + request.getMessageId());
        IncomingRequest incoming = requests.get(request.getMessageId());
        if (incoming == null) {
            incoming = new IncomingRequest(this);
            requests.put(request.getMessageId(), incoming);
            this.actor.addEvent(new ActorRequestStored(request));
        } else {
//            System.out.println("Gettging same request again, ignoring it.");
//            actor.getLogger().warn("Received same request again");
        }
    }

    public void updateState(ModelActorRequestEvent event) {
        IncomingRequest incoming = requests.computeIfAbsent(event.getMessageId(), k -> new IncomingRequest(this));
        incoming.updateState(event);
    }

    public void recoveryCompleted() {
        requests.values().forEach(IncomingRequest::recoveryCompleted);
    }
}
