package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.command.exception.AuthorizationException;
import org.cafienne.actormodel.command.exception.CommandException;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.command.response.CommandFailure;
import org.cafienne.actormodel.command.response.EngineChokedFailure;
import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.actormodel.command.response.SecurityFailure;
import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class CommandHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends ValidMessageHandler<C, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    protected final C command;

    public CommandHandler(A actor, C msg) {
        super(actor, msg);
        this.command = msg;
        addDebugInfo(() -> "\n\n\txxxxxxxxxxxxxxxxxxxx new command " + command.getCommandDescription() +" xxxxxxxxxxxxxxx\n\n", logger);
    }

    protected Logger getLogger() {
        return logger;
    }

    /**
     * Runs the case security checks on user context and case tenant.
     */
    @Override
    protected AuthorizationException runSecurityChecks() {
        AuthorizationException issue = validateUserAndTenant();
        if (issue != null) {
            addDebugInfo(() -> issue, logger);
            setNextResponse(new SecurityFailure(command, issue));
        }

        return issue;
    }

    @Override
    protected void process() {
        addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getCommandDescription() , command.toJson(), getLogger());

        // First, simple, validation
        try {
            command.validate(actor);
        } catch (AuthorizationException e) {
            addDebugInfo(() -> e, logger);
            setNextResponse(new SecurityFailure(getCommand(), e));
            logger.debug("===== Command was not authorized ======");
            return;
        } catch (InvalidCommandException e) {
            addDebugInfo(() -> e, logger);
            setNextResponse(new CommandFailure(getCommand(), e));
            logger.debug("===== Command was invalid ======");
            return;
        } catch (Throwable e) {
            addDebugInfo(() -> e, logger);
            setNextResponse(new EngineChokedFailure(getCommand(), e));
            addDebugInfo(() -> "---------- Engine choked during validation of command with type " + command.getClass().getSimpleName() + " from user " + command.getUser().id() + " in actor " + this.actor.getId() + "\nwith exception", logger);
            return;
        }

        try {
            // Leave the actual work of processing to the command itself.
            setNextResponse(command.process(actor));
            logger.info("---------- User " + command.getUser().id() + " in " + this.actor + " completed command " + command);
        } catch (AuthorizationException e) {
            addDebugInfo(() -> e, logger);
            setNextResponse(new SecurityFailure(getCommand(), e));
            logger.debug("===== Command was not authorized ======");
        } catch (CommandException e) {
            setNextResponse(new CommandFailure(getCommand(), e));
            addDebugInfo(() -> "---------- User " + command.getUser().id() + " in actor " + this.actor.getId() + " failed to complete command " + command + "\nwith exception", logger);
            addDebugInfo(() -> e, logger);
        } catch (Throwable e) {
            setNextResponse(new EngineChokedFailure(getCommand(), e));
            addDebugInfo(() -> "---------- Engine choked during processing of command with type " + command.getClass().getSimpleName() + " from user " + command.getUser().id() + " in actor " + this.actor.getId() + "\nwith exception", logger);
            addDebugInfo(() -> e, logger);
        }
    }

    @Override
    protected final void complete() {
        // Handling the incoming message can result in 3 different scenarios that are dealt with below:
        // 1. The message resulted in an exception that needs to be returned to the client; Possibly the case must be restarted.
        // 2. The message did not result in state changes (e.g., when fetching discretionary items), and the response can be sent straight away
        // 3. The message resulted in state changes, so the new events need to be persisted, and after persistence the response is sent back to the client.

        if (hasFailures()) { // Means there is a response AND it is of type CommandFailure
            // Inform the sender about the failure
            // In case of failure we still want to store the debug events. Actually, mostly we need this in case of failure (what else are we debugging for)
            Object[] debugEvents = events.stream().filter(e -> e instanceof DebugEvent).toArray();
            actor.replyAndThenPersistEvents(Arrays.asList(debugEvents), response);


            // If we have created events (other than debug events) from the failure, then we are in inconsistent state and need to restart the actor.
            if (events.size() > debugEvents.length) {
                Throwable exception = ((CommandFailure) response).internalException();
                addDebugInfo(() -> {
                    StringBuilder msg = new StringBuilder("\n------------------------ SKIPPING PERSISTENCE OF " + events.size() + " EVENTS IN " + this);
                    events.forEach(e -> msg.append("\n\t"+e.getDescription()));
                    return msg + "\n";
                }, logger);
                addDebugInfo(() -> exception, logger);
                actor.failedWithInvalidState(this, exception);
            }
        } else {
            // Follow regular procedure
            super.complete();
        }
    }

    protected void setNextResponse(ModelResponse response) {
        this.response = response;
    }

    protected boolean hasFailures() {
        return response != null && response instanceof CommandFailure;
    }

    public C getCommand() {
        return command;
    }
}
