package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.EngineChokedFailure;
import org.cafienne.actormodel.response.SecurityFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler extends IncomingMessageHandler {
    private final static Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    protected final ModelCommand command;

    protected final ModelActor actor;

    public CommandHandler(ModelActor actor, ModelCommand msg) {
        super(actor, msg);
        this.actor = actor;
        this.command = msg;
        addDebugInfo(() -> "\n\n\txxxxxxxxxxxxxxxxxxxx new command " + command.getCommandDescription() +" xxxxxxxxxxxxxxx\n\n", getLogger());
    }

    private Logger getLogger() {
        return logger;
    }

    @Override
    protected void process() {
        addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + actor + " starts command " + command.getCommandDescription() , command.toJson(), getLogger());

        // First, simple, validation
        try {
            command.validateCommand(actor);
        } catch (AuthorizationException e) {
            addDebugInfo(() -> e, getLogger());
            setResponse(new SecurityFailure(getCommand(), e));
            return;
        } catch (InvalidCommandException e) {
            addDebugInfo(() -> e, getLogger());
            setResponse(new CommandFailure(getCommand(), e));
            addDebugInfo(() -> "===== Command was invalid ======", getLogger());
            return;
        } catch (Throwable e) {
            addDebugInfo(() -> e, getLogger());
            setResponse(new EngineChokedFailure(getCommand(), e));
            addDebugInfo(() -> "---------- Engine choked during validation of command with type " + command.getClass().getSimpleName() + " from user " + command.getUser().id() + " in " + this.actor + "\nwith exception", getLogger());
            return;
        }

        try {
            // Leave the actual work of processing to the command itself.
            setResponse(command.processCommand(actor));
        } catch (AuthorizationException e) {
            addDebugInfo(() -> e, getLogger());
            setResponse(new SecurityFailure(getCommand(), e));
        } catch (CommandException e) {
            setResponse(new CommandFailure(getCommand(), e));
            addDebugInfo(() -> "---------- User " + command.getUser().id() + " in " + this.actor + " failed to complete command " + command + "\nwith exception", getLogger());
            addDebugInfo(() -> e, getLogger());
        } catch (Throwable e) {
            setResponse(new EngineChokedFailure(getCommand(), e));
            addDebugInfo(() -> "---------- Engine choked during processing of command with type " + command.getClass().getSimpleName() + " from user " + command.getUser().id() + " in " + this.actor + "\nwith exception", getLogger());
            addDebugInfo(() -> e, getLogger());
        }
    }

    protected void beforeCommit() {
        if (events.containStateChanges()) {
            command.done();
        }
    }

    public ModelCommand getCommand() {
        return command;
    }

    @Override
    protected void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        actor.reply(new CommandFailure(command, new Exception("Handling the request resulted in a system failure. Check the server logs for more information.")));
    }
}
