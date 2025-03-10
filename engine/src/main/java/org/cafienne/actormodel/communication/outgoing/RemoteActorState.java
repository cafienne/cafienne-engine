package org.cafienne.actormodel.communication.outgoing;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.outgoing.command.RequestModelActor;
import org.cafienne.actormodel.communication.outgoing.event.ActorRequestDelivered;
import org.cafienne.actormodel.communication.outgoing.event.ActorRequestCreated;
import org.cafienne.actormodel.communication.outgoing.response.ActorRequestFailed;
import org.cafienne.actormodel.communication.outgoing.response.ActorRequestDeliveryReceipt;
import org.cafienne.actormodel.communication.outgoing.response.ModelActorSystemResponse;
import org.cafienne.actormodel.response.ModelResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the state of interacting with the remote actor as perceived within the local actor
 *
 * @param <LocalActor> A typesafe version of the local actor, not of the remote one (!).
 */
public abstract class RemoteActorState<LocalActor extends ModelActor> {
    public final LocalActor actor;
    public final String targetActorId;
    private final Map<String, Request> requests = new HashMap<>();

    protected RemoteActorState(LocalActor actor, String targetActorId) {
        this.actor = actor;
        this.targetActorId = targetActorId;
        this.actor.register(this);
    }

    public void sendRequest(ModelCommand command) {
        new Request(this, command).send();
    }

    public final void registerDelivery(ModelActorSystemResponse response) {
        updateRequest(response.getMessageId(), r -> r.delivered());
        if (response instanceof ActorRequestDeliveryReceipt receipt) {
            handleReceipt(receipt);
        } else if (response instanceof ActorRequestFailed failure) {
            handleFailure(failure);
        }
    }

    public void handleReceipt(ActorRequestDeliveryReceipt receipt) {
        actor.addEvent(new ActorRequestDelivered(this, receipt));
    }

    public abstract void handleFailure(ActorRequestFailed actorRequestFailed);

    public String getDescription() {
        return this.getClass().getSimpleName() + "[" + targetActorId + "]";
    }

    public final void updateState(ActorRequestCreated event) {
        updateRequest(event.getMessageId(), r -> r.submitted(event));
    }

    private void updateRequest(String messageId, RequestAction action) {
        Request request = requests.get(messageId);
        if (request != null) {
            action.perform(request);
        }
    }

    class Request {
        private final RemoteActorState<LocalActor> state;
        private final ModelCommand command;
        private boolean completed = false;

        Request(RemoteActorState<LocalActor> state, ModelCommand command) {
            this.state = state;
            this.command = command;
            requests.put(command.getMessageId(), this);
        }

        void send() {
            actor.addEvent(new ActorRequestCreated(state, command));
        }

        public void submitted(ActorRequestCreated event) {
            if (! completed) {
//                System.out.println(this + ": request is not yet completed, sending it to " + state.targetActorId);
                actor.caseSystem.gateway().inform(new RequestModelActor(command, state), actor.self());
            }
        }

        public void delivered() {
//            System.out.println(this +" is delivered. Completed was: " + this.completed);
            this.completed = true;
//            System.out.println("Completed now is: " + this.completed);
        }

        @Override
        public String toString() {
            return state.actor +": " + this.command.getCommandDescription();
        }
    }
}

@FunctionalInterface
interface RequestAction {
    void perform(RemoteActorState<?>.Request request);
}
