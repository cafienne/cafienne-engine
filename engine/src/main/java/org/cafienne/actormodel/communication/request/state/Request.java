package org.cafienne.actormodel.communication.request.state;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.request.command.RequestModelActor;
import org.cafienne.actormodel.communication.request.event.ActorRequestCreated;
import org.cafienne.actormodel.communication.request.event.ActorRequestDelivered;
import org.cafienne.actormodel.communication.request.event.ModelActorReplyEvent;
import org.cafienne.processtask.actorapi.command.MigrateProcessDefinition;
import org.cafienne.processtask.actorapi.command.ReactivateProcess;

public class Request {
    private final RemoteActorState<?> state;
    private final RequestTracker tracker;
    private ActorRequestCreated creationEvent;
    private ActorRequestDelivered deliveryEvent;

    Request(RemoteActorState<?> state) {
        this.state = state;
        this.tracker = new RequestTracker(this, state.actor.caseSystem);
    }

    void updateState(ModelActorReplyEvent event) {
        switch (event) {
            case ActorRequestCreated arc -> created(arc);
            case ActorRequestDelivered ard -> delivered(ard);
            default -> System.out.println("BOOM on a " + event.getClass().getName());
        }
    }

    public ModelCommand getCommand() {
        return creationEvent.command;
    }

    public void created(ActorRequestCreated event) {
        this.creationEvent = event;
        if (state.actor.recoveryFinished()) {
            send();
        }
    }

    public void delivered(ActorRequestDelivered event) {
//        System.out.println(this +" is delivered upon count " + tracker.count());
        this.deliveryEvent = event;
        tracker.stop();
    }

    public void send() {
        //        System.out.println(this + ": request is not yet completed, sending it to " + state.targetActorId);
        this.state.actor.caseSystem.gateway().inform(new RequestModelActor(creationEvent.command, state), state.actor.self());
        tracker.start();
    }

    void failed() {
        state.requestDeliveryFailed(this);
    }

    @Override
    public String toString() {
        return state.actor + ": " + creationEvent.command.getCommandDescription();
    }

    final void recoveryCompleted() {
        if (creationEvent != null && deliveryEvent == null) {
//            System.out.println(this + ": CALLING SEND UPON RECOVERY COMPLTED TO " + state.targetActorId);
            send();
        }
    }
}
