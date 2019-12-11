package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.MessageHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends MessageHandler<C, C, E, A> {
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
            actor.addDebugInfo(() -> e);
            setNextResponse(new SecurityFailure(msg, issue));
        }

        return issue;
    }

    @Override
    protected void process() {
        getLogger().info("---------- User " + command.getUser().id() + " in case " + this.actor.getId() + " starts command " + command);

//        addDebugInfo(DebugEvent.class, e -> e.addMessage("---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getClass().getSimpleName(), command.toJson()));
        addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getClass().getSimpleName(), getLogger());

        // First, simple, validation
        try {
            command.validate(actor);
        } catch (SecurityException e) {
            actor.addDebugInfo(() -> e);
            setNextResponse(new SecurityFailure(getCommand(), e));
            logger.debug("===== Command was not authorized ======");
            return;
        } catch (InvalidCommandException e) {
            actor.addDebugInfo(() -> e);
            setNextResponse(new CommandFailure(getCommand(), e));
            logger.debug("===== Command was invalid ======");
            return;
        }

        try {
            // Leave the actual work of processing to the command itself.
            setNextResponse(command.process(actor));
            getLogger().info("---------- User " + command.getUser().id() + " in case " + this.actor.getId() + " completed command " + command);
        } catch (CommandException e) {
            setNextResponse(new CommandFailure(getCommand(), e));
            getLogger().info("---------- User " + command.getUser().id() + " in case " + this.actor.getId() + " failed to complete command " + command + "\nwith exception", e);
            actor.addDebugInfo(() -> "---------- User " + command.getUser().id() + " in actor " + this.actor.getId() + " failed to complete command " + command + "\nwith exception");
            actor.addDebugInfo(() -> e);
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

    protected boolean hasFailures() {
        return response != null && response instanceof CommandFailure;
    }

    public C getCommand() {
        return command;
    }
}
