package org.cafienne.actormodel.communication.reply.state;

import org.cafienne.actormodel.communication.reply.event.ActorRequestExecuted;
import org.cafienne.actormodel.communication.reply.event.ActorRequestFailed;
import org.cafienne.actormodel.communication.reply.event.ActorRequestStored;
import org.cafienne.actormodel.communication.reply.event.ModelActorRequestEvent;

class IncomingRequest {

    private final IncomingRequestState state;
    private ActorRequestStored stored;
    private ActorRequestExecuted executed;
    private ActorRequestFailed failed;

    IncomingRequest(IncomingRequestState state) {
        this.state = state;
    }

    void updateState(ModelActorRequestEvent event) {
//        if (state.actor.recoveryRunning()) {
//            System.out.println("RECOVERING " + state.actor + " with " + event.getDescription());
//        }
        switch (event) {
            case ActorRequestStored ars -> this.stored = ars;
            case ActorRequestExecuted are -> this.executed = are;
            case ActorRequestFailed arf -> this.failed = arf;
            default -> throw new IllegalStateException("Unexpected value: " + event);
        }
    }

    void recoveryCompleted() {
        if (this.stored != null && (this.executed == null && this.failed == null)) {
            // Calling again.
//            System.out.println("SENDING STORED AGAIN?!");
            this.stored.afterPersist(state.actor);
        }
    }
}
