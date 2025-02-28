package org.cafienne.actormodel.communication.outgoing;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.outgoing.command.RequestModelActor;
import org.cafienne.actormodel.communication.outgoing.event.ActorRequestDelivered;
import org.cafienne.actormodel.communication.outgoing.response.ActorRequestFailed;
import org.cafienne.actormodel.communication.outgoing.response.ActorRequestDeliveryReceipt;

/**
 * Represents the state of interacting with the remote actor as perceived within the local actor
 *
 * @param <LocalActor> A typesafe version of the local actor, not of the remote one (!).
 */
public abstract class RemoteActorState<LocalActor extends ModelActor> {
    public final LocalActor actor;
    public final String targetActorId;

    protected RemoteActorState(LocalActor actor, String targetActorId) {
        this.actor = actor;
        this.targetActorId = targetActorId;
        this.actor.register(this);
    }

    public void sendRequest(ModelCommand command) {
        actor.caseSystem.gateway().inform(new RequestModelActor(command, this), actor.self());
    }

    public void handleReceipt(ActorRequestDeliveryReceipt receipt) {
        actor.addEvent(new ActorRequestDelivered(this, receipt));
    }

    public abstract void handleFailure(ActorRequestFailed actorRequestFailed);

    public String getDescription() {
        return this.getClass().getSimpleName();
    }
}
