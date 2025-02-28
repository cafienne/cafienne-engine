package org.cafienne.actormodel;

import org.cafienne.actormodel.communication.reply.state.IncomingRequestState;
import org.cafienne.actormodel.communication.request.state.RemoteActorState;

import java.util.HashMap;
import java.util.Map;

public class ModelActorCommunication {
    private final ModelActor actor;
    private final IncomingRequestState incomingRequests;
    private final Map<String, RemoteActorState<?>> remoteActors = new HashMap<>();

    ModelActorCommunication(ModelActor actor) {
        this.actor = actor;
        this.incomingRequests = new IncomingRequestState(actor);

    }

    void register(RemoteActorState<?> remoteActorState) {
        this.remoteActors.put(remoteActorState.targetActorId, remoteActorState);
    }

    RemoteActorState<?> getRemoteActorState(String actorId) {
        return remoteActors.get(actorId);
    }

    IncomingRequestState getIncomingRequestState() {
        return incomingRequests;
    }

    void recoveryCompleted() {
        incomingRequests.recoveryCompleted();
        remoteActors.forEach((string, state) -> state.recoveryCompleted());


    }
}
