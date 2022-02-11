package org.cafienne.actormodel;

import akka.actor.Cancellable;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.*;
import org.cafienne.infrastructure.Cafienne;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Place that handles valid incoming traffic ({@link IncomingActorMessage})
 * It also makes the ModelActor to go back to sleep if it has been idle for a while.
 */
class BackOffice {
    private final ModelActor actor;
    private final Reception reception;

    BackOffice(ModelActor actor, Reception reception) {
        this.actor = actor;
        this.reception = reception;
    }

    void handleVisitor(IncomingActorMessage message) {
        // Steps:
        // 1. Remove self cleaner
        // 2. Handle message
        //  a. ModelCommand --> handle the command
        //  b. ModelResponse --> handle the response
        // 3. Tell the staging area we're done (storing events and sending replies)
        // 4. Set a new self cleaner (basically resets the timer)
        clearSelfCleaner();

        StagingArea stagingArea = reception.warehouse.prepareNextShipment(message);
        if (message.isCommand()) {
            ModelCommand command = message.asCommand();
            actor.addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getCommandDescription() , command.toJson());

            try {
                // First, simple, validation
                command.validateCommand(actor);
                // Then, do actual work of processing in the command itself.
                stagingArea.setResponse(command.processCommand(actor));
            } catch (AuthorizationException e) {
                stagingArea.reportFailure(e, new SecurityFailure(command, e), "");
            } catch (InvalidCommandException e) {
                stagingArea.reportFailure(command, e, "===== Command was invalid ======");
            } catch (CommandException e) {
                stagingArea.reportFailure(command, e, "---------- User " + command.getUser().id() + " in " + this.actor + " failed to complete command " + command + "\nwith exception");
            } catch (Throwable e) {
                stagingArea.reportFailure(e, new ActorChokedFailure(command, e),"---------- Engine choked during validation of command with type " + command.getClass().getSimpleName() + " from user " + command.getUser().id() + " in " + this.actor + "\nwith exception");
            }
        } else if (message instanceof ModelResponse) {
            handleResponse((ModelResponse) message);
        }

        stagingArea.store();

        enableSelfCleaner();
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
            if (msg instanceof CommandFailure) {
                handler.left.handleFailure((CommandFailure) msg);
            } else {
                handler.right.handleResponse(msg);
            }
        }
    }

    /**
     * SelfCleaner provides a mechanism to have the ModelActor remove itself from memory after a specific idle period.
     */
    private Cancellable selfCleaner = null;

    private void clearSelfCleaner() {
        // Receiving message should reset the self-cleaning timer
        if (selfCleaner != null) {
            selfCleaner.cancel();
            selfCleaner = null;
        }
    }

    private void enableSelfCleaner() {
        if (actor.hasAutoShutdown()) {
            // Now set the new selfCleaner
            long idlePeriod = Cafienne.config().actor().idlePeriod();
            FiniteDuration duration = Duration.create(idlePeriod, TimeUnit.MILLISECONDS);
            selfCleaner = actor.getScheduler().schedule(duration, actor::takeABreak);
        }
    }
}