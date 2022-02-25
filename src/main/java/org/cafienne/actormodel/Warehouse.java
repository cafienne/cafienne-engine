package org.cafienne.actormodel;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.slf4j.Logger;

/**
 * Warehouse creates a new {@link StagingArea} for each {@link IncomingActorMessage}.
 */
class Warehouse {
    private final ModelActor actor;
    private StagingArea staging;
    private boolean isOpen = false;

    Warehouse(ModelActor actor) {
        this.actor = actor;
    }

    StagingArea prepareNextShipment(IncomingActorMessage message) {
        isOpen = true;
        actor.setCurrentUser(message.getUser());
        staging = new StagingArea(actor, message);
        return staging;
    }

    void storeEvent(ModelEvent event) {
        if (isOpen) {
            staging.addEvent(event);
        } else {
            // Recovery is still running.

            // NOTE: ModelActors should not generate events during recovery.
            //  Such has been implemented for TenantActor and ProcessTaskActor, and partly for Case.
            //  Enabling the logging will showcase where this pattern has not been completely done.
            if (EngineDeveloperConsole.enabled()) {
                EngineDeveloperConsole.debugIndentedConsoleLogging("!!! Recovering " + actor + " generates event of type " + event.getClass().getSimpleName());
            }
        }
    }

    void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        if (isOpen) {
            staging.handlePersistFailure(cause, event, seqNr);
        }
    }

    /**
     * Add debug info to the ModelActor if debug is enabled.
     * If the actor runs in debug mode (or if slf4j has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param logger The slf4j logger instance to check whether debug logging is enabled
     * @param appender A functional interface returning "an" object, holding the main info to be logged.
     *                 Note: the interface is only invoked if logging is enabled. This appender typically
     *                 returns a String that is only created upon demand (in order to speed up a bit)
     * @param additionalInfo Additional objects to be logged. Typically, pointers to existing objects.
     */
    void addDebugInfo(Logger logger, DebugInfoAppender appender, Object... additionalInfo) {
        if (isOpen) {
            staging.addDebugInfo(logger, appender, additionalInfo);
        }
    }
}
