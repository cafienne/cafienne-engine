package org.cafienne.cmmn.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.cmmn.akka.event.*;
import org.cafienne.cmmn.akka.event.plan.PlanItemCreated;
import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.akka.event.plan.task.TaskEvent;
import org.cafienne.cmmn.akka.event.plan.PlanItemEvent;
import org.cafienne.cmmn.akka.event.plan.task.TaskInputFilled;
import org.cafienne.cmmn.akka.event.plan.task.TaskOutputFilled;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.assertions.PublishedEventsAssertion;
import org.cafienne.cmmn.test.filter.EventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The CaseEventListener reads and stores ModelEvents
 */
public class CaseEventListener {
    private final static Logger logger = LoggerFactory.getLogger(CaseEventListener.class);

    private List<ModelEvent> publishedEvents = new ArrayList();
    private List<ModelEvent> newEvents = new ArrayList();
    private CaseModified lastCaseModifiedEvent;
    private final ActorRef caseMessageRouter; // proxy to the case system
    private final ActorRef responseHandlingActor; // The actor we use to communicate with the case system
    private final TestScript testScript;
    private final CaseEventPublisher readJournal;

    CaseEventListener(TestScript testScript) {
        this.testScript = testScript;
        // Case message router is used to send messages into the case system
        this.caseMessageRouter = CaseSystem.router();

        final ActorSystem system = CaseSystem.system();
        // Now create the callback mechanism for the case system
        this.responseHandlingActor = system.actorOf(Props.create(ResponseHandlingActor.class, this.testScript));
        // And create a connection with the Akka Event database to receive events from the case system
        this.readJournal = new CaseEventPublisher(this, system);
    }

    void sendCommand(Object command) {
        newEvents = new ArrayList();
        caseMessageRouter.tell(command, responseHandlingActor);
    }

    void handle(Object object) {
        if (object instanceof ModelEvent) {
            if (object instanceof CaseModified) {
                lastCaseModifiedEvent = (CaseModified) object;
            }
            handle((ModelEvent) object);
        } else {
            logger.warn("Received unexpected event " + object);
        }
    }

    private void handle(ModelEvent event) {
        logger.debug("Received "+event.getClass().getSimpleName()+" event " + event);
        publishedEvents.add(event);
        newEvents.add(event);
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Returns a copy of the list of events that have been received since the last command was sent.
     *
     * @return
     */
    public PublishedEventsAssertion getNewEvents() {
        return new PublishedEventsAssertion(new ArrayList(newEvents));
    }

    /**
     * Returns a copy of the list of events that have been generated until now.
     *
     * @return
     */
    public PublishedEventsAssertion getEvents() {
        return new PublishedEventsAssertion(new ArrayList(publishedEvents));
    }

    /**
     * Wait until the case modified event for the corresponding timestamp has come.
     * @param timestamp
     * @return
     */
    public CaseModified awaitCaseModifiedEvent(Instant timestamp) {
        // First check whether it is already there.
        //  This happens typically when a CaseTestCommand did not create new events in the Case
        if (lastCaseModifiedEvent != null && timestamp.equals(lastCaseModifiedEvent.lastModified())) {
            return lastCaseModifiedEvent;
        } else {
            // Now wait for the event stream listener to have handled upcoming events.
            return waitUntil("CaseModified-"+timestamp, CaseModified.class, event -> timestamp.equals(event.lastModified()));
        }
    }

    public CaseModified awaitTaskModified(Instant timestamp) {
        // Now wait for the event stream listener to have handled upcoming events.
        return waitUntil("TaskModified-"+timestamp, CaseModified.class, event -> timestamp.equals(event.lastModified()));
    }

    /**
     * Generic listener for a plan item event of the specified type where the plan item has the matching identifier
     *
     * @param identifier       name or id of plan item that it's looking for
     * @param tClass           Type of event class we're searching for
     * @param filter
     * @param optionalDuration
     * @param <T>
     * @return
     */
    public <T extends PlanItemEvent> T awaitPlanItemEvent(String identifier, Class<T> tClass, EventFilter<T> filter, long... optionalDuration) {
        return waitUntil("PlanItemEvent-"+identifier, tClass, event -> {
            if (event.getPlanItemId().equals(identifier)) {
                logger.debug("Matching event for plan item "+identifier+" of type "+event.getType()+" for filter. Event "+event);
            }
            return (! ((!event.getPlanItemId().equals(identifier) && !this.hasPlanItemName(event.getPlanItemId(), identifier)) || !filter.matches(event)));
        }, optionalDuration);
    }

    private <T extends TaskEvent> T awaitTaskEvent(String identifier, Class<T> tClass, EventFilter<T> filter, long... optionalDuration) {
        return waitUntil("TaskEvent-"+identifier, tClass, event -> {
            if (event.getTaskId().equals(identifier) || hasPlanItemName(event.getTaskId(), identifier)) {
                logger.debug("Receiving event "+event);
            }
            return (! ((!event.getTaskId().equals(identifier) && !this.hasPlanItemName(event.getTaskId(), identifier)) || !filter.matches(event)));
        }, optionalDuration);
    }

    public boolean hasPlanItemName(String id, String name) {
        long count = this.getEvents()
                .filter(evt -> evt instanceof PlanItemCreated) // Only plan item created
                .filter(evt -> ((PlanItemCreated) evt).getPlanItemId().equals(id)) // With this id
                .filter(evt -> ((PlanItemCreated) evt).getPlanItemName().equals(name)).getEvents().size(); // having this name
        return count > 0;
    }

    /**
     * Returns the event that caused the plan item with the specified identifier to go into the expected state.
     *
     * @param identifier
     * @param state
     * @param optionalDuration
     * @return
     */
    public PlanItemTransitioned awaitPlanItemState(String identifier, State state, long... optionalDuration) {
        return awaitPlanItemTransitioned(identifier, e -> e.getCurrentState().equals(state), optionalDuration);
    }

    /**
     * Returns the event that caused the plan item with the specified identifier to go from the history state into the current state via the last transition.
     *
     * @param identifier
     * @param currentState
     * @param optionalDuration
     * @return
     */
    public PlanItemTransitioned awaitPlanItemState(String identifier, Transition lastTransition, State currentState, State historyState, long... optionalDuration) {
        return awaitPlanItemTransitioned(identifier, e -> e.getCurrentState().equals(currentState) && e.getTransition().equals(lastTransition) && e.getHistoryState().equals(historyState), optionalDuration);
    }

    /**
     * Wait until a plan item event for the plan item with the specified id (or name) matches the filter
     *
     * @param identifier
     * @param filter
     * @param optionalDuration
     * @return
     */
    public PlanItemTransitioned awaitPlanItemTransitioned(String identifier, EventFilter<PlanItemTransitioned> filter, long... optionalDuration) {
        return awaitPlanItemEvent(identifier, PlanItemTransitioned.class, filter, optionalDuration);
    }

    /**
     * With for the plan item with the specified id to have generated an TaskInputFilled event
     *
     * @param identifier
     * @param filter
     * @param optionalDuration
     * @return
     */
    public TaskInputFilled awaitTaskInputFilled(String identifier, EventFilter<TaskInputFilled> filter, long... optionalDuration) {
        return awaitTaskEvent(identifier, TaskInputFilled.class, filter, optionalDuration);
    }

    /**
     * With for the plan item with the specified id to have generated an TaskOutputFilled event
     *
     * @param identifier
     * @param filter
     * @param optionalDuration
     * @return
     */
    public TaskOutputFilled awaitTaskOutputFilled(String identifier, EventFilter<TaskOutputFilled> filter, long... optionalDuration) {
        return awaitTaskEvent(identifier, TaskOutputFilled.class, filter, optionalDuration);
    }

    /**
     * Waits for the first event that matches the EventFilter.
     * Note: this takes all events in the event history into account as well, not just the ones
     * that are yet to be published.
     *
     * @param tClass           The type of event we're searching for
     * @param filter
     * @param optionalDuration An optional duration; default is 10000 milliseconds.
     * @return
     */
    public <T extends ModelEvent> T waitUntil(Class<T> tClass, EventFilter<T> filter, long... optionalDuration) {
        return waitUntil("", tClass, filter, optionalDuration);
    }

    public <T extends ModelEvent> T waitUntil(String msg, Class<T> tClass, EventFilter<T> filter, long... optionalDuration) {
        long duration = optionalDuration.length >= 1 ? optionalDuration[0] : 10000;
        long remainingDuration = duration;
        long remainingDurationInSeconds = remainingDuration / 1000;
        long startOfWait = System.currentTimeMillis();
        synchronized (this) {
            int numPublishedEvents = publishedEvents.size();
            // First check if we find a match for the filter on the current set of events;
            for (int i = 0; i < numPublishedEvents; i++) {
                ModelEvent event = publishedEvents.get(i);
                if (filterMatches(tClass, filter, event)) {
                    return (T) event;
                }
            }

            while (true) {
                try {
                    // Wait in trenches of seconds if nothing happens on the event stream
                    wait(1000);

                    long now = System.currentTimeMillis();
                    remainingDuration = duration - (now - startOfWait);

                    int currentNumEvents = publishedEvents.size();
                    for (int i = numPublishedEvents; i < currentNumEvents; i++) {

                        ModelEvent event = publishedEvents.get(i);
                        if (filterMatches(tClass, filter, event)) {
                            return (T) event;
                        }
                    }

                    numPublishedEvents = currentNumEvents;

                    if (now - startOfWait > duration) {
                        throw new AssertionError("Events have not come after waiting for more than " + duration / 1000 + " seconds");
                    }

                    // Usually events are received immediately; so only print this message if we really need to wait (because then probably they
                    // are not coming due to some configuration error or so)
                    if (remainingDurationInSeconds != remainingDuration / 1000) {
                        remainingDurationInSeconds = remainingDuration/1000;
                        logger.warn("Waiting " + remainingDurationInSeconds + " seconds for match "+msg+ " on filter " + filter);
                    }
                } catch (InterruptedException e) {
                    logger.warn("Breaking out with interrupted exception", e);
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Simple helper method
     */
    private <T extends ModelEvent> boolean filterMatches(Class<T> tClass, EventFilter<T> filter, ModelEvent event) {
        return tClass.isAssignableFrom(event.getClass()) && filter.matches((T) event);
    }
}