package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.CommandException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.exception.MissingTenantException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.command.response.SecurityFailure;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;

public class CommandHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends ValidMessageHandler<C, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    protected final C command;
    protected ModelResponse response;

    /**
     * User context passed with the current command or event.
     */
    private TenantUser user;

    public CommandHandler(A actor, C msg) {
        super(actor, msg);
        this.user = msg.getUser();
        this.command = msg;
    }

    protected Logger getLogger() {
        return logger;
    }

    /**
     * Runs the case security checks on user context and case tenant.
     */
    @Override
    final protected InvalidCommandException runSecurityChecks() {
        InvalidCommandException issue = null;
        // Security checks:
        // - need a proper user.
        // - is the case tenant already available, and is the user in that same tenant?
        // Same exception is needed for security reasons: do not expose more info than needed.
        if (user == null || user.id() == null || user.id().trim().isEmpty()) {
            issue = new InvalidCommandException("The user id must not be null or empty");
        } else if (user.tenant() == null) {
            issue = new MissingTenantException("User must provide tenant information in order to execute a command");
        } else if (actor.getTenant() != null && !actor.getTenant().equals(user.tenant())) {
            issue = new InvalidCommandException("Cannot handle command " + getClass().getName() + "; StartCase not yet sent, or not in this user context");
        }

        if (issue != null) {
            final Exception e = issue;
            addDebugInfo(() -> e, logger);
            setNextResponse(new SecurityFailure(msg, issue));
        }

        return issue;
    }

    @Override
    protected void process() {
        addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getClass().getSimpleName(), command.toJson(), getLogger());

        // First, simple, validation
        try {
            command.validate(actor);
        } catch (SecurityException e) {
            addDebugInfo(() -> e, logger);
            setNextResponse(new SecurityFailure(getCommand(), e));
            logger.debug("===== Command was not authorized ======");
            return;
        } catch (InvalidCommandException e) {
            addDebugInfo(() -> e, logger);
            setNextResponse(new CommandFailure(getCommand(), e));
            logger.debug("===== Command was invalid ======");
            return;
        }

        try {
            // Leave the actual work of processing to the command itself.
            setNextResponse(command.process(actor));
            logger.info("---------- User " + command.getUser().id() + " in " + this.actor + " completed command " + command);
        } catch (SecurityException e) {
            addDebugInfo(() -> e, logger);
            setNextResponse(new SecurityFailure(getCommand(), e));
            logger.debug("===== Command was not authorized ======");
        } catch (CommandException e) {
            setNextResponse(new CommandFailure(getCommand(), e));
            addDebugInfo(() -> "---------- User " + command.getUser().id() + " in actor " + this.actor.getId() + " failed to complete command " + command + "\nwith exception", logger);
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
            if (actor.getLastModified() != null) {
                response.setLastModified(actor.getLastModified());
            }

            // Inform the sender about the failure
            actor.reply(response);

            // In case of failure we still want to store the debug events. Actually, mostly we need this in case of failure (what else are we debugging for)
            Object[] debugEvents = events.stream().filter(e -> e instanceof DebugEvent).toArray();
            if (debugEvents.length > 0) {
                actor.persistEvents(Arrays.asList(debugEvents));
            }

            // If we have created events (other than debug events) from the failure, then we are in inconsistent state and need to restart the actor.
            if (events.size() > debugEvents.length) {
                Throwable exception = ((CommandFailure) response).internalException();
                actor.failedWithInvalidState(this, exception);
            }
        } else if (hasOnlyDebugEvents()) { // Nothing to persist, just respond to the client if there is something to say
            if (response != null) {
                response.setLastModified(actor.getLastModified());
                // Now tell the sender about the response
                actor.reply(response);
                // Also store the debug events if there are
                actor.persistEvents(events);
            }
        } else {
            // We have events to persist.
            //  Add a "transaction" event at the last moment
            checkEngineVersion();

            // Change the last modified moment of this case; update it in the response, and publish an event about it
            Instant lastModified = Instant.now();
            addEvent(actor.createLastModifiedEvent(lastModified));
            if (response != null) {
                response.setLastModified(lastModified);
            }
            actor.persistEventsAndThenReply(events, response);
        }
    }

    /**
     * Returns the user context of the command processed in this handler
     *
     * @return
     */
    public TenantUser getUser() {
        return user;
    }

    protected void setNextResponse(ModelResponse response) {
        this.response = response;
    }

    protected boolean hasOnlyDebugEvents() {
        boolean hasOnlyDebugEvents = ! events.stream().anyMatch(e -> ! (e instanceof DebugEvent));
        return hasOnlyDebugEvents;
    }

    protected boolean hasFailures() {
        return response != null && response instanceof CommandFailure;
    }

    public C getCommand() {
        return command;
    }
}
