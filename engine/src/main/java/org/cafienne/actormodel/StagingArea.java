/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.actormodel;

import org.apache.pekko.persistence.journal.Tagged;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.CommitEvent;
import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.EngineChokedFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.infrastructure.EngineVersion;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.json.Value;
import org.cafienne.system.health.HealthMonitor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StagingArea captures all state changing events upon handling
 * an {@link IncomingActorMessage}
 * It also handles failures and sending responses to complete the lifecycle of the message.
 */
public class StagingArea {
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
        EngineVersion actorVersion = actor.getEngineVersion();
        EngineVersion currentEngineVersion = actor.caseSystem.version();
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
        addDebugInfo(actor.getLogger(), () -> "Updating actor state for new event "+ event.getDescription());
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
            actor.completeMessageHandling(message, this);
            if (hasStatefulEvents()) {
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
            actor.persistAsync(tag(debugEvent), e -> {});
        }
    }

    private Object tag(ModelEvent event) {
        return new Tagged(event, event.tags());
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

        // Apply tagging to the events
        final List<Object> taggedEvents = events.stream().map(this::tag).collect(Collectors.toList());
        // When the last event is persisted, we can send a reply. Keep track of that last event here, so that we need not go through the list each time.
        final Object lastTaggedEvent = taggedEvents.get(taggedEvents.size() - 1);

        actor.persistAll(taggedEvents, persistedEvent -> {
            HealthMonitor.writeJournal().isOK();
            if (getLogger().isDebugEnabled()) {
                if (persistedEvent instanceof Tagged) {
                    Object e = ((Tagged) persistedEvent).payload();
                    getLogger().debug(actor + " - persisted event [" + actor.lastSequenceNr() + "] of type " + e.getClass().getName());
                } else {
                    getLogger().debug(actor + " - persisted event [" + actor.lastSequenceNr() + "] of type " + persistedEvent.getClass().getName());
                }
            }
            if (persistedEvent == lastTaggedEvent) {
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
        actor.addDebugInfo(() -> "", exception, msg);
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
        return !events.isEmpty();
    }

    /**
     * If the last event is not a CommitEvent we need one.
     * @return
     */
    boolean needsCommitEvent() {
        return hasStatefulEvents() && !(events.get(events.size() - 1) instanceof CommitEvent);
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
        if (logDebugMessages(logger)) {
            // Ensure log message is only generated once.
            Object info = appender.info();

            // Check whether the first additional parameter can be appended to the main info or deserves a separate
            //  logging entry. Typically, this is done for exceptions and json objects and arrays.
            if (additionalInfo.length == 0 || needsSeparateLine(additionalInfo[0])) {
                logObject(logger, info);
                logObjects(logger, additionalInfo);
            } else {
                additionalInfo[0] = String.valueOf(info) + additionalInfo[0];
                logObjects(logger, additionalInfo);
            }
        }
    }

    private boolean needsSeparateLine(Object additionalObject) {
        return additionalObject instanceof Throwable ||
                additionalObject instanceof Value && !((Value<?>) additionalObject).isPrimitive();
    }

    private void logObjects(Logger logger, Object... any) {
        for (Object o : any) {
            logObject(logger, o);
        }
    }

    private void logObject(Logger logger, Object o) {
        if (o instanceof Throwable) {
            logException(logger, (Throwable) o);
        } else if (o instanceof Value) {
            logJSON(logger, (Value<?>) o);
        } else {
            // Value of will also convert a null object to "null", so no need to check on null.
            logString(logger, String.valueOf(o));
        }
    }

    private void logString(Logger logger, String logMessage) {
        if (logMessage.isBlank()) {
            return;
        }
        logger.debug(logMessage); // plain slf4j
        EngineDeveloperConsole.debugIndentedConsoleLogging(logMessage); // special dev indentation in console
        getDebugEvent().addMessage(logMessage); // when actor runs in debug mode also publish events
    }

    private void logJSON(Logger logger, Value<?> json) {
        logger.debug(json.toString());
        EngineDeveloperConsole.debugIndentedConsoleLogging(json);
        getDebugEvent().addMessage(json);
    }

    private void logException(Logger logger, Throwable t) {
        logger.debug(t.getMessage(), t);
        EngineDeveloperConsole.debugIndentedConsoleLogging(t);
        getDebugEvent().addMessage(t);
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
