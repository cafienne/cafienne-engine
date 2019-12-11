package org.cafienne.tenant;

import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.tenant.akka.command.TenantCommand;
import org.cafienne.tenant.akka.event.TenantEvent;
import org.cafienne.tenant.akka.event.TenantModified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;

public class TenantCommandHandler extends CommandHandler<TenantCommand, TenantEvent, TenantActor> {
    private final static Logger logger = LoggerFactory.getLogger(TenantCommandHandler.class);

    public TenantCommandHandler(TenantActor actor, TenantCommand msg) {
        super(actor, msg);
    }

    @Override
    protected void complete() {
        actor.setLastModified(Instant.now());
        if (hasFailures()) {
            // Inform the sender about the failure
            sender().tell(response, self());

            // In case of failure we still want to store the debug events. Actually, mostly we need this in case of failure (what else are we debugging for)
            Object[] debugEvents = events.stream().filter(e -> e instanceof DebugEvent).toArray();
            if (debugEvents.length > 0) {
                actor.persistAll(Arrays.asList(debugEvents), e -> {});
            }
        } else {
            if (events.stream().filter(e -> !(e instanceof DebugEvent)).count() > 0) {
                // Only add modified event if there are new events that are not debug events
                addEvent(new TenantModified(actor, actor.getLastModified()));
            }
            actor.persistAll(events, e -> {
                logger.debug("Tenant " + actor.getId() + " persisted event of type " + e.getClass().getSimpleName());
                if (e instanceof TenantModified) {
                    if (response != null) {
                        logger.debug("Tenant persisted all events out of command " + command.getClass().getName() + ", responding to sender with a message of type " + response.getClass().getName() + ".");
                    } else {
                        logger.debug("Tenant persisted all events out of command " + command.getClass().getName() + ". No response to sender?!");
                    }
                    sender().tell(response, self());
                } else {
                }
            });
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
