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

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.CommandFailureListener;
import org.cafienne.actormodel.response.CommandResponseListener;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Warehouse creates a new {@link ModelActorTransaction} for each {@link IncomingActorMessage}.
 */
class BackOffice {
    private final ModelActor actor;
    private final ModelActorMonitor monitor;
    private ModelActorTransaction currentTransaction;
    private boolean isOpen = false;

    /**
     * Registration of listeners that are interacting with (other) models through this case.
     */
    private final Map<String, Responder> responseListeners = new HashMap<>();

    BackOffice(ModelActor actor, ModelActorMonitor monitor) {
        this.actor = actor;
        this.monitor = monitor;
    }

    void performTransaction(IncomingActorMessage message) {
        // Tell the actor monitor we're busy
        monitor.setBusy();

        isOpen = true;
        currentTransaction = new ModelActorTransaction(actor, this, message);
        currentTransaction.perform();

        // Tell the actor monitor we're free again
        monitor.setFree();
    }

    void askModel(ModelCommand command, CommandFailureListener left, CommandResponseListener right) {
        if (actor.recoveryRunning()) {
//            System.out.println("Ignoring request to send command of type " + command.getClass().getName()+" because recovery is running");
            return;
        }
        synchronized (responseListeners) {
            responseListeners.put(command.getMessageId(), new Responder(command, left, right));
        }
        actor.addDebugInfo(() -> "----------" + this + " sends command " + command.getDescription(), command.rawJson());

        actor.caseSystem.gateway().inform(command, actor.self());
    }

    void handleResponse(ModelResponse msg) {
        Responder handler = getResponseListener(msg.getMessageId());
        if (handler == null) {
            // For all commands that are sent to another case via us, a listener is registered.
            // If that listener is null, we set a default listener ourselves.
            // So if we still do not find a listener, it means that we received a response to a command that we never submitted,
            // and we log a warning for that. It basically means someone else has submitted the command and told the other case to respond to us -
            // which is strange.
            actor.getLogger().warn(actor + " received a response to a message that was not sent through it. Sender: " + actor.sender() + ", response: " + msg);
        } else {
            actor.addDebugInfo(() -> actor + " received response to command of type " + handler.command.getDescription(), msg.rawJson());
            if (msg instanceof CommandFailure) {
                handler.left.handleFailure((CommandFailure) msg);
            } else {
                handler.right.handleResponse(msg);
            }
        }
    }

    private Responder getResponseListener(String msgId) {
        synchronized (responseListeners) {
            return responseListeners.remove(msgId);
        }
    }

    void storeEvent(ModelEvent event) {
        if (isOpen) {
            currentTransaction.addEvent(event);
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
            currentTransaction.handlePersistFailure(cause, event, seqNr);
        }
    }

    /**
     * Add debug info to the ModelActor if debug is enabled.
     * If the actor runs in debug mode (or if slf4j has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param logger         The slf4j logger instance to check whether debug logging is enabled
     * @param appender       A functional interface returning "an" object, holding the main info to be logged.
     *                       Note: the interface is only invoked if logging is enabled. This appender typically
     *                       returns a String that is only created upon demand (in order to speed up a bit)
     * @param additionalInfo Additional objects to be logged. Typically, pointers to existing objects.
     */
    void addDebugInfo(Logger logger, DebugInfoAppender appender, Object... additionalInfo) {
        if (isOpen) {
            currentTransaction.addDebugInfo(logger, appender, additionalInfo);
        }
    }
}
