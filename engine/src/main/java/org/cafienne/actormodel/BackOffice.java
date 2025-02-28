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
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.ActorChokedFailure;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.actormodel.response.SecurityFailure;

/**
 * Place that handles valid incoming traffic ({@link IncomingActorMessage})
 * It also makes the ModelActor to go back to sleep if it has been idle for a while.
 */
class BackOffice {
    private final ModelActor actor;
    private final Reception reception;
    private final ModelActorMonitor monitor;

    BackOffice(ModelActor actor, Reception reception) {
        this.actor = actor;
        this.reception = reception;
        this.monitor = actor.monitor;
    }

    void handleVisitor(IncomingActorMessage message) {
        // Steps:
        // 1. Remove self cleaner
        // 2. Handle message
        //  a. ModelCommand --> handle the command
        //  b. ModelResponse --> handle the response
        // 3. Tell the staging area we're done (storing events and sending replies)
        // 4. Set a new self cleaner (basically resets the timer)
        monitor.actorActivated();

        ModelActorTransaction modelActorTransaction = reception.warehouse.createTransaction(message);
        if (message.isCommand()) {
            ModelCommand command = message.asCommand();
            actor.addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getDescription() , command.rawJson());

            try {
                // First, simple, validation
                command.validateCommand(actor);
                // Then, do actual work of processing in the command itself.
                command.processCommand(actor);
                modelActorTransaction.setResponse(command.getResponse());
            } catch (AuthorizationException e) {
                modelActorTransaction.reportFailure(e, new SecurityFailure(command, e), "");
            } catch (InvalidCommandException e) {
                modelActorTransaction.reportFailure(command, e, "===== Command was invalid ======");
            } catch (CommandException e) {
                modelActorTransaction.reportFailure(command, e, "---------- User " + command.getUser().id() + " in " + this.actor + " failed to complete command " + command + "\nwith exception");
            } catch (Throwable e) {
                modelActorTransaction.reportFailure(e, new ActorChokedFailure(command, e),"---------- Engine choked during validation of command with type " + command.getClass().getSimpleName() + " from user " + command.getUser().id() + " in " + this.actor + "\nwith exception");
            }
        } else if (message.isResponse()) {
            handleResponse(message.asResponse());
        }

        modelActorTransaction.commit();

        monitor.actorDeactivated();
    }

    private void handleResponse(ModelResponse msg) {
        Responder handler = actor.getResponseListener(msg.getMessageId());
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
}
