package org.cafienne.actormodel;

import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.system.health.HealthMonitor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class EventBatch {
    private final ModelActor actor;
    private final MessageHandler handler;
    private final static int avgNumEvents = 30;
    protected final List<ModelEvent> events = new ArrayList<>(avgNumEvents);
    private DebugEvent debugEvent;

    public EventBatch(ModelActor actor, MessageHandler handler) {
        this.actor = actor;
        this.handler = handler;
    }

    void addEvent(ModelEvent event) {
        events.add(event);
    }

    private Logger getLogger() {
        return actor.getLogger();
    }

    public void commit(ModelResponse response) {
        // If there are only debug events, first respond and then persist the events (for performance).
        // Otherwise, only send a response upon successful persisting the events.
        if (events.isEmpty()) {
            replyAndPersistDebugEvent(response);
        } else {
            persistEventsAndThenReply(response);
        }
    }

    public void abortWith(Object message, ModelResponse response) {
        // Inform the sender about the failure and then store the debug event if any
        replyAndPersistDebugEvent(response);

        // If we have created events (other than debug events) from the failure, then we are in inconsistent state and need to restart the actor.
        if (! events.isEmpty()) {
            Throwable exception = ((CommandFailure) response).internalException();
            handler.addDebugInfo(() -> {
                StringBuilder msg = new StringBuilder("\n------------------------ SKIPPING PERSISTENCE OF " + events.size() + " EVENTS IN " + actor);
                events.forEach(e -> msg.append("\n\t").append(e.getDescription()));
                return msg + "\n";
            }, getLogger());
            handler.addDebugInfo(() -> exception, getLogger());
            actor.failedWithInvalidState(message, exception);
        }
    }

    private void replyAndPersistDebugEvent(ModelResponse response) {
        // Inform the sender about the failure
        actor.reply(response);
        // In case of failure we still want to store the debug event. Actually, mostly we need this in case of failure (what else are we debugging for)
        if (handler.hasPersistence() && hasDebugEvent()) {
            actor.persistAsync(debugEvent, e -> {});
        }
    }

    private void persistEventsAndThenReply(ModelResponse response) {
        if (getLogger().isDebugEnabled() || EngineDeveloperConsole.enabled()) {
            StringBuilder msg = new StringBuilder("\n------------------------ PERSISTING " + events.size() + " EVENTS IN " + actor);
            events.forEach(e -> msg.append("\n\t").append(e));
            getLogger().debug(msg + "\n");
            EngineDeveloperConsole.debugIndentedConsoleLogging(msg + "\n");
        }
        // Include the debug event if any.
        if (hasDebugEvent()) {
            events.add(0, debugEvent);
        }
        if (events.isEmpty() || !handler.hasPersistence()) {
            actor.reply(response);
        } else {
            final Object lastEvent = events.get(events.size() - 1);
            actor.persistAll(events, e -> {
                HealthMonitor.writeJournal().isOK();
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug(actor.getDescription() + " - persisted event [" + actor.lastSequenceNr() + "] of type " + e.getClass().getName());
                }
                if (e == lastEvent) {
                    actor.reply(response);
                }
            });
        }
    }

    /**
     * Get or create the debug event for this message handler.
     * Only one debug event per handler, containing all debug messages.
     * @return
     */
    DebugEvent getDebugEvent() {
        if (debugEvent == null) {
            debugEvent = new DebugEvent(actor);
        }
        return debugEvent;
    }

    /**
     * Returns true if debug info is available, and also if the
     * actor runs in debug mode and is not in recovery
     * @return
     */
    private boolean hasDebugEvent() {
        return debugEvent != null && actor.debugMode() && !actor.recoveryRunning();
    }

    /**
     * Simplistic
     * @return
     */
    public boolean containStateChanges() {
        return events.size() > 0;
    }
}
