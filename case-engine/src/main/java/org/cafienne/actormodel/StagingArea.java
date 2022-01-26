package org.cafienne.actormodel;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.EngineChokedFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.instance.debug.DebugExceptionAppender;
import org.cafienne.cmmn.instance.debug.DebugJsonAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.json.Value;
import org.cafienne.system.health.HealthMonitor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * StagingArea captures all state changing events upon handling
 * an {@link IncomingActorMessage}
 * It also handles failures and sending responses to complete the lifecycle of the message.
 */
class StagingArea {
    private final ModelActor actor;
    private final static int avgNumEvents = 30;
    private final List<ModelEvent> events = new ArrayList<>(avgNumEvents);
    private DebugEvent debugEvent;
    private final IncomingActorMessage message;
    private ModelResponse response = null;

    StagingArea(ModelActor actor, IncomingActorMessage message) {
        this.actor = actor;
        this.message = message;
        // First check the engine version, potentially leading to an extra event.
        this.checkEngineVersion();
    }

    private void checkEngineVersion() {
        // First check whether the engine version has changed or not; this may lead to an EngineVersionChanged event
        CafienneVersion currentEngineVersion = Cafienne.version();
        CafienneVersion actorVersion = actor.getEngineVersion();
        if (actorVersion != null && currentEngineVersion.differs(actor.getEngineVersion())) {
            actor.getLogger().info(actor + " changed engine version from\n" + actor.getEngineVersion()+ " to\n" + currentEngineVersion);
            addEvent(new EngineVersionChanged(actor, currentEngineVersion));
        }
    }

    /**
     * Add an event and update the actor state for it.
     */
    void addEvent(ModelEvent event) {
        events.add(event);
        addDebugInfo(() -> "Updating actor state for new event "+ event.getDescription(), actor.getLogger());
        event.updateActorState(actor);
    }

    private Logger getLogger() {
        return actor.getLogger();
    }

    /**
     * Store events in persistence and optionally reply a response to the sender of the incoming message.
     */
    void store() {
        // Handling the incoming message can result in 3 different scenarios that are dealt with below:
        // 1. The message resulted in an exception that needs to be returned to the client; Possibly the case must be restarted.
        // 2. The message resulted in state changes, so the new events need to be persisted, and after persistence the response is sent back to the client.
        // 3. The message did not result in state changes (e.g., when fetching discretionary items), and the response can be sent straight away
        if (hasFailures()) {
            // Inform the sender about the failure and then store the debug event if any
            replyAndPersistDebugEvent(response);

            // If we have created events (other than debug events) from the failure, then we are in inconsistent state and need to restart the actor.
            if (hasStatefulEvents()) {
                Throwable exception = ((CommandFailure) response).internalException();
                actor.addDebugInfo(() -> {
                    StringBuilder msg = new StringBuilder("\n------------------------ ABORTING PERSISTENCE OF " + events.size() + " EVENTS IN " + actor);
                    events.forEach(e -> msg.append("\n\t").append(e.getDescription()));
                    return msg + "\n";
                }, exception);
                actor.failedWithInvalidState(message, exception);
            }
        } else {
            // If there are only debug events, first respond and then persist the events (for performance).
            // Otherwise, only send a response upon successful persisting the events.
            if (hasStatefulEvents()) {
                actor.completeTransaction(message);
                persistEventsAndThenReply(response);
            } else {
                replyAndPersistDebugEvent(response);
            }
        }
    }

    private void replyAndPersistDebugEvent(ModelResponse response) {
        // Inform the sender about the failure
        actor.reply(response);
        // In case of failure we still want to store the debug event. Actually, mostly we need this in case of failure (what else are we debugging for)
        if (hasDebugEvent()) {
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
        final Object lastEvent = events.get(events.size() - 1);
        actor.persistAll(events, e -> {
            HealthMonitor.writeJournal().isOK();
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(actor + " - persisted event [" + actor.lastSequenceNr() + "] of type " + e.getClass().getName());
            }
            if (e == lastEvent) {
                actor.reply(response);
            }
        });
    }

    /**
     * When back office encounters command handling failures, they can
     * report it to the staging area.
     * Stateful events are not stored, DebugEvent would still get stored.
     */
    void reportFailure(ModelCommand command, Throwable exception, String msg) {
        reportFailure(exception, new CommandFailure(command, exception), msg);
    }

    /**
     * When back office encounters command handling failures, they can
     * report it to the staging area.
     * Stateful events are not stored, DebugEvent would still get stored.
     */
    void reportFailure(Throwable exception, CommandFailure failure, String msg) {
        actor.addDebugInfo(() -> "", exception);
        actor.addDebugInfo(() -> msg);
        this.response = failure;
    }

    /**
     * To be invoked upon successful command handling.
     */
    void setResponse(ModelResponse response) {
        this.response = response;
    }

    /**
     * Hook to optionally tell the sender about persistence failures
     * @param cause
     * @param event
     * @param seqNr
     */
    void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        if (message.isCommand()) {
            actor.reply(new EngineChokedFailure(message.asCommand(), new Exception("Handling the request resulted in a system failure. Check the server logs for more information.")));
        }
    }

    /**
     * Get or create the debug event for this batch.
     * Only one debug event per handler, containing all debug messages.
     */
    private DebugEvent getDebugEvent() {
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

    private boolean hasFailures() {
        return response instanceof CommandFailure;
    }

    /**
     * Simplistic
     * @return
     */
    private boolean hasStatefulEvents() {
        return events.size() > 0;
    }

    void addDebugInfo(DebugStringAppender appender, Value<?> json, Logger logger) {
        addDebugInfo(appender, logger);
        addDebugInfo(json::cloneValueNode, logger);
    }

    void addDebugInfo(DebugStringAppender appender, Throwable exception, Logger logger) {
        addDebugInfo(appender, logger);
        addDebugInfo(() -> exception, logger);
    }

    /**
     * Add debug info to the case if debug is enabled.
     * If the case runs in debug mode (or if Log4J has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param appender
     */
    void addDebugInfo(DebugStringAppender appender, Logger logger) {
        // First check whether at all we should add some message.
        if (logDebugMessages(logger)) {
            // Ensure log message is only generated once.
            String logMessage = appender.debugInfo();
            if (! logMessage.isBlank()) { // Ignore blank messages
                logger.debug(logMessage); // plain log4j
                EngineDeveloperConsole.debugIndentedConsoleLogging(logMessage); // special dev indentation in console
                getDebugEvent().addMessage(logMessage); // when actor runs in debug mode also publish events
            }
        }
    }

    void addDebugInfo(DebugJsonAppender appender, Logger logger) {
        if (logDebugMessages(logger)) {
            Value<?> json = appender.info();
            logger.debug(json.toString());
            EngineDeveloperConsole.debugIndentedConsoleLogging(json);
            getDebugEvent().addMessage(json);
        }
    }

    void addDebugInfo(DebugExceptionAppender appender, Logger logger) {
        if (logDebugMessages(logger)) {
            Throwable t = appender.exceptionInfo();
            logger.debug(t.getMessage(), t);
            EngineDeveloperConsole.debugIndentedConsoleLogging(t);
            getDebugEvent().addMessage(t);
        }
    }

    /**
     * Whether to write log messages.
     * Log messages are created through invocation of FunctionalInterface, and if this
     * method returns false, those interfaces are not invoked, in an attempt to improve runtime performance.
     * @return
     */
    private boolean logDebugMessages(Logger logger) {
        return EngineDeveloperConsole.enabled() || logger.isDebugEnabled() || actor.debugMode();
    }
}
