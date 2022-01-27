package org.cafienne.actormodel;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.cmmn.instance.debug.DebugJsonAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.json.Value;
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

    void addDebugInfo(DebugStringAppender appender, Value<?> json, Logger logger) {
        if (isOpen) {
            staging.addDebugInfo(appender, json, logger);
        }
    }

    void addDebugInfo(DebugStringAppender appender, Logger logger) {
        if (isOpen) {
            staging.addDebugInfo(appender, logger);
        }
    }

    void addDebugInfo(DebugStringAppender appender, Throwable exception, Logger logger) {
        if (isOpen) {
            staging.addDebugInfo(appender, exception, logger);
        }
    }

    void addDebugInfo(DebugJsonAppender appender, Logger logger) {
        if (isOpen) {
            staging.addDebugInfo(appender, logger);
        }
    }
}
