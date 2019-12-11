package org.cafienne.cmmn.instance;

import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.event.CaseModified;
import org.cafienne.cmmn.akka.event.EngineVersionChanged;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CaseCommandHandler extends CommandHandler<CaseCommand, CaseInstanceEvent, Case> {
    private final static Logger logger = LoggerFactory.getLogger(CaseCommandHandler.class);
    private final Case caseInstance;

    CaseCommandHandler(Case actor, CaseCommand msg) {
        super(actor, msg);
        this.caseInstance = actor;
    }

    @Override
    protected void complete() {
        // Handling the incoming message can result in 3 different scenarios that are dealt with below:
        // 1. The message resulted in an exception that needs to be returned to the client; Possibly the case must be restarted.
        // 2. The message did not result in state changes (e.g., when fetching discretionary items), and the response can be sent straight away
        // 3. The message resulted in state changes, so the new events need to be persisted, and after persistence the response is sent back to the client.

        boolean hasOnlyDebugEvents = ! events.stream().anyMatch(e -> ! (e instanceof DebugEvent));

        if (hasFailures()) { // Means there is a response AND it is of type CommandFailure
            if (caseInstance.getLastModified() != null) {
                response.setLastModified(caseInstance.getLastModified());
            }

            logger.debug("Sending back response msg: "+ response);

            // Inform the sender about the failure
            sender().tell(response, self());

            // In case of failure we still want to store the debug events. Actually, mostly we need this in case of failure (what else are we debugging for)
            Object[] debugEvents = events.stream().filter(e -> e instanceof DebugEvent).toArray();
            if (debugEvents.length > 0) {
                actor.persistAll(Arrays.asList(debugEvents), e -> {});
            }

            // If we have created events (other than debug events) from the failure, then we are in inconsistent state and need to restart the actor.
            if (events.size() > debugEvents.length) {
                caseInstance.getScheduler().clearSchedules(); // Remove all schedules.
                Throwable exception = ((CommandFailure) response).internalException();
                logger.error("Encountered failure in handling msg of type " + command.getClass().getName() + "; restarting case " + actor.getId(), exception);
                caseInstance.supervisorStrategy().restartChild(self(), exception, true);
            }
        } else if (hasOnlyDebugEvents) { // Nothing to persist, just respond to the client if there is something to say
            if (response != null) {
                response.setLastModified(caseInstance.getLastModified());
                // Now connect the sender about the response
                sender().tell(response, self());
                caseInstance.persistAll(events, e -> afterEventPersisted(e, null));
            }
        } else {
            // We have events to persist.
            //  Add a "transaction" event at the last moment
            //  Also validate the engine version ... (should we always do this?)

            // TTDL: this belongs in the generic ModelActor logic
            // First check whether the engine version has changed or not; this may lead to an EngineVersionChanged event
            EngineVersionChanged v = caseInstance.checkEngineVersion();
            if (v != null) {
                events.add(0, v);
            }

            // Change the last modified moment of this case; update it in the response, and publish an event about it
            Instant lastModified = Instant.now();
            caseInstance.setLastModified(lastModified);

            if (response != null) {
                response.setLastModified(lastModified);
            }
            storeInternallyGeneratedEvent(new CaseModified(caseInstance, lastModified, caseInstance.failingPlanItems.size())).finished();

            // Now persist the events in one shot
            logger.debug("Persisting " + events.size() + " events out of command " + command);
            caseInstance.persistAll(events, (ModelEvent e) -> afterEventPersisted(e, response));
        }
    }

    /**
     * This methods is invoked by the case to buffer the events that originate from a command
     *
     * @param event
     */
    <T extends CaseInstanceEvent> T storeInternallyGeneratedEvent(T event) {
        addEvent(event);
        super.addDebugInfo(() -> "Adding " + event.getClass().getSimpleName() + " event " + events.size(), logger);
        event.setParent(currentEvent);
        currentEvent = event;
        return event;
    }

    /**
     * Handler after event has been persisted. After last event of a command has been persisted we send the response
     * back to the client (if any)
     *
     * @param event
     * @param response
     */
    private void afterEventPersisted(ModelEvent event, ModelResponse response) {
        logger.debug("Case[" + actor.getId() + "] persisted event of type " + event.getClass().getSimpleName());
        if (event instanceof CaseModified && response != null) {
            sender().tell(response, caseInstance.self());
        }
    }

    /**
     * Internal framework method, used to set the current event back to it's parent if the current event finished
     *
     * @param event
     */
    void setCurrentEvent(CaseInstanceEvent event) {
        currentEvent = event;
    }

    CaseInstanceEvent getCurrentEvent() {
        return currentEvent;
    }

    /**
     * Helper member to keep track of execution stack. (Events can cause other events)
     */
    private CaseInstanceEvent currentEvent = null;


    @Override
    public Logger getLogger() {
        return logger;
    }
}
