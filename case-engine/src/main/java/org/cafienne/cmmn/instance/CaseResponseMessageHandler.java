package org.cafienne.cmmn.instance;

import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.handler.ResponseHandler;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.event.CaseModified;
import org.cafienne.cmmn.akka.event.EngineVersionChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class CaseResponseMessageHandler extends ResponseHandler<CaseCommand, CaseInstanceEvent, Case> {
    private final static Logger logger = LoggerFactory.getLogger(CaseResponseMessageHandler.class);
    private final ModelResponse command;
    private final Case caseInstance;

    CaseResponseMessageHandler(Case actor, ModelResponse msg) {
        super(actor, msg);
        this.caseInstance = actor;
        this.command = msg;
    }

    @Override
    protected void complete() {
        // Handling the incoming message can result in 3 different scenarios that are dealt with below:
        // 1. The message resulted in an exception that needs to be returned to the client; Possibly the case must be restarted.
        // 2. The message did not result in state changes (e.g., when fetching discretionary items), and the response can be sent straight away
        // 3. The message resulted in state changes, so the new events need to be persisted, and after persistence the response is sent back to the client.

        if (!events.isEmpty()) {
            // We have events to persist.
            //  Add a "transaction" event at the last moment
            //  Also validate the engine version ... (should we always do this?)

            // First check whether the engine version has changed or not; this may lead to an EngineVersionChanged event
            EngineVersionChanged v = caseInstance.checkEngineVersion();
            if (v != null) {
                events.add(0, v);
            }

            // Change the last modified moment of this case; update it in the response, and publish an event about it
            Instant lastModified = Instant.now();
            caseInstance.setLastModified(lastModified);

            storeInternallyGeneratedEvent(new CaseModified(caseInstance, lastModified, caseInstance.failingPlanItems.size())).finished();

            // Now persist the events in one shot
            logger.debug("Persisting " + events.size() + " events out of command " + command);
            caseInstance.persistAll(events, event -> logger.debug("Case[" + getId() + "] persisted event of type " + event.getClass().getSimpleName()));
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

    String getId() {
        return caseInstance.getId();
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

}
