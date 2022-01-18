package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoveryEventHandler extends ValidMessageHandler {
    /**
     * The message that was sent to the actor that is being handled by this handler.
     */
    protected final ModelEvent msg;

    protected final ModelActor actor;

    private final static Logger logger = LoggerFactory.getLogger(RecoveryEventHandler.class);

    public RecoveryEventHandler(ModelActor actor, ModelEvent msg) {
        super(actor, msg);
        this.actor = actor;
        this.msg = msg;
    }

    protected void process() {
        msg.recover(actor);
    }

    @Override
    public <T extends ModelEvent> T addEvent(T event) {
        // NOTE: This commit makes it possible to handle actor state updates from events only.
        //  Such has been implemented for TenantActor and ProcessTaskActor, and partly for Case.
        //  Enabling the logging will showcase where this pattern has not been completely done.
//        System.out.println("Recovering " + actor + " generates event of type " + event.getClass().getSimpleName());

        logger.debug("Not storing internally generated event because recovery is running. Event is of type " + event.getClass().getSimpleName());
        return event;
    }

    protected void complete() {
    }
}
