package org.cafienne.processtask.instance;

import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.processtask.akka.command.ProcessCommand;
import org.cafienne.processtask.akka.event.ProcessInstanceEvent;
import org.cafienne.processtask.akka.event.ProcessModified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class ProcessTaskCommandHandler extends CommandHandler<ProcessCommand, ProcessInstanceEvent, ProcessTaskActor> {
    private final static Logger logger = LoggerFactory.getLogger(ProcessTaskCommandHandler.class);

    public ProcessTaskCommandHandler(ProcessTaskActor actor, ProcessCommand msg) {
        super(actor, msg);
    }

    @Override
    protected void complete() {
        actor.setLastModified(Instant.now());
        addEvent(new ProcessModified(actor, actor.getLastModified()));
        actor.persistAll(events, e -> {
            logger.debug("Task persisted event of type " + e.getClass().getSimpleName());
            if (e instanceof ProcessModified) {
                if (response != null) {
                    logger.debug("Task persisted all events out of command "+command.getClass().getName()+", responding to sender with a message of type " + response.getClass().getName() + ".");
                } else {
                    logger.debug("Task persisted all events out of command "+command.getClass().getName()+". No response to sender?!");
                }
                sender().tell(response, self());
            } else {
            }
        });
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
