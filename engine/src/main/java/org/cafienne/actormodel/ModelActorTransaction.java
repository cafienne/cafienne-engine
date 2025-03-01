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
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.*;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.infrastructure.EngineVersion;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.system.health.HealthMonitor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ModelActorTransaction captures all state changing events upon handling an {@link IncomingActorMessage}
 * It also handles failures and sending responses to complete the lifecycle of the message.
 */
public class ModelActorTransaction {
    private final ModelActor actor;
    private final static int avgNumEvents = 30;
    private final List<ModelEvent> events = new ArrayList<>(avgNumEvents);
    private final IncomingActorMessage message;
    private final BackOffice backOffice;
    private ModelResponse response = null;
    private final TransactionLogger logger;

    ModelActorTransaction(ModelActor actor, BackOffice backOffice, IncomingActorMessage message) {
        this.actor = actor;
        this.backOffice = backOffice;
        this.actor.setCurrentUser(message.getUser());
        this.message = message;
        this.logger = new TransactionLogger(this, actor);
        // First check the engine version, potentially leading to an extra event.
        this.checkEngineVersion();
    }

    void perform() {
        if (message.isCommand()) {
            ModelCommand command = message.asCommand();
            actor.addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getDescription() , command.rawJson());

            try {
                // First, simple, validation
                command.validateCommand(actor);
                // Then, do actual work of processing in the command itself.
                command.processCommand(actor);
                setResponse(command.getResponse());
            } catch (AuthorizationException e) {
                reportFailure(e, new SecurityFailure(command, e), "");
            } catch (InvalidCommandException e) {
                reportFailure(command, e, "===== Command was invalid ======");
            } catch (CommandException e) {
                reportFailure(command, e, "---------- User " + command.getUser().id() + " in " + this.actor + " failed to complete command " + command + "\nwith exception");
            } catch (Throwable e) {
                reportFailure(e, new ActorChokedFailure(command, e),"---------- Engine choked during validation of command with type " + command.getClass().getSimpleName() + " from user " + command.getUser().id() + " in " + this.actor + "\nwith exception");
            }
        } else if (message.isResponse()) {
            backOffice.handleResponse(message.asResponse());
        }

        commit();
    }

    private void checkEngineVersion() {
        // First check whether the engine version has changed or not; this may lead to an EngineVersionChanged event
        EngineVersion actorVersion = actor.getEngineVersion();
        EngineVersion currentEngineVersion = actor.caseSystem.version();
        if (actorVersion != null && currentEngineVersion.differs(actor.getEngineVersion())) {
            actor.getLogger().info(actor + " changed engine version from\n" + actor.getEngineVersion() + " to\n" + currentEngineVersion);
            addEvent(new EngineVersionChanged(actor, currentEngineVersion));
        }
    }

    /**
     * Add an event and update the actor state for it.
     */
    void addEvent(ModelEvent event) {
        events.add(event);
        addDebugInfo(getLogger(), () -> "Updating actor state for new event " + event.getDescription());
        event.updateActorState(actor);
    }

    private Logger getLogger() {
        return actor.getLogger();
    }

    /**
     * Store events in persistence and optionally reply a response to the sender of the incoming message.
     */
    void commit() {
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
        if (this.logger.hasDebugEvent()) {
            actor.persistAsync(addTags(logger.getDebugEvent()), e -> {});
        }
    }

    private Tagged addTags(ModelEvent event) {
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
        if (logger.hasDebugEvent()) {
            events.addFirst(logger.getDebugEvent());
        }

        // Apply tagging to the events
        final List<Tagged> taggedEvents = events.stream().map(this::addTags).collect(Collectors.toList());
        // When the last event is persisted, we can send a reply. Keep track of that last event here, so that we need not go through the list each time.
        final Tagged lastTaggedEvent = taggedEvents.getLast();

        actor.persistAll(taggedEvents, persistedEvent -> {
            HealthMonitor.writeJournal().isOK();
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(actor + " - persisted event [" + actor.lastSequenceNr() + "] of type " + persistedEvent.payload().getClass().getName());
            }
            if (persistedEvent == lastTaggedEvent) {
                actor.reply(response);
                events.forEach(event -> event.afterPersist(actor));
            }
        });
    }

    /**
     * When back office encounters command handling failures, they can report it here.
     * Stateful events are not stored, DebugEvent would still get stored.
     */
    void reportFailure(ModelCommand command, Throwable exception, String msg) {
        reportFailure(exception, new CommandFailure(command, exception), msg);
    }

    /**
     * When back office encounters command handling failures, they can report it here.
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
     */
    void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        if (message.isCommand()) {
            actor.reply(new EngineChokedFailure(message.asCommand(), new Exception("Handling the request resulted in a system failure. Check the server logs for more information.")));
        }
    }

    private boolean hasFailures() {
        return response instanceof CommandFailure;
    }

    /**
     * Simplistic
     */
    private boolean hasStatefulEvents() {
        return !events.isEmpty();
    }

    /**
     * If the last event is not a CommitEvent we need one.
     */
    boolean needsCommitEvent() {
        return hasStatefulEvents() && !(events.getLast() instanceof CommitEvent);
    }

    /**
     * Add debug info to the ModelActor if debug is enabled.
     * If the actor runs in debug mode (or if slf4j has debug enabled for this logger),
     * then the appender.debugInfo(...) method will be invoked to store a string in the log.
     *
     * @param logger         The slf4j logger instance to check whether debug logging is enabled
     * @param appender       A functional interface returning "an" object, holding the main info to be logged.
     *                       Note: the interface is only invoked if logging is enabled. This appender typically
     *                       returns a String that is only created upon demand (in order to speed up a bit)
     * @param additionalInfo Additional objects to be logged. Typically, pointers to existing objects.
     */
    void addDebugInfo(Logger logger, DebugInfoAppender appender, Object... additionalInfo) {
        this.logger.addDebugInfo(logger, appender, additionalInfo);
    }
}
