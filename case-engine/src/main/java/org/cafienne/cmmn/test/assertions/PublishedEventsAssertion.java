package org.cafienne.cmmn.test.assertions;

import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.cmmn.akka.event.CaseFileEvent;
import org.cafienne.cmmn.akka.event.CaseModified;
import org.cafienne.cmmn.instance.CaseInstanceEvent;
import org.cafienne.cmmn.test.CaseTestCommand;
import org.cafienne.cmmn.test.filter.EventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Set of assertions around the events that have been published after the test script has handled a command.
 * Events are taken from {@link CaseTestCommand#getEventListener()}.
 */
public class PublishedEventsAssertion<E extends ModelEvent> {
    private final List<E> events;
    private final static Logger logger = LoggerFactory.getLogger(PublishedEventsAssertion.class);

    public PublishedEventsAssertion(List<E> publishedEvents) {
        this.events = publishedEvents;
    }

    /**
     * Asserts the number of events that have been published
     *
     * @param expectedNumberOfEvents
     * @return
     */
    public PublishedEventsAssertion assertSize(int expectedNumberOfEvents) {
        if (events.size() != expectedNumberOfEvents) {
            throw new AssertionError("Expecting " + expectedNumberOfEvents + ", but found " + events.size() + " events");
        }
        return this;
    }

    /**
     * Assert the set of events is not empty
     * @return
     */
    public PublishedEventsAssertion assertNotEmpty() {
        if (events.isEmpty()) {
            throw new AssertionError("Expecting events, but found none");
        }
        return this;
    }

    /**
     * Simple helper method
     */
    private <T extends ModelEvent> boolean filterMatches(Class<T> tClass, EventFilter<T> filter, ModelEvent event) {
        return tClass.isAssignableFrom(event.getClass()) && filter.matches((T) event);
    }

    /**
     * Assert that there is an event that matches the class and the filter.
     * @param msg
     * @param tClass
     * @param filter
     * @param <T>
     * @return
     */
    public <T extends ModelEvent> T assertEvent(String msg, Class<T> tClass, EventFilter<T> filter) {
        // First check if we find a match for the filter on the current set of events;
        for (int i = 0; i < events.size(); i++) {
            ModelEvent event = events.get(i);
            if (filterMatches(tClass, filter, event)) {
                return (T) event;
            }
        }
        throw new AssertionError(msg);
    }

    private String cleanPath(String path) {
        int openBracket = path.indexOf('[');
        int closeBracket = path.indexOf(']');
        if (openBracket > 0 && closeBracket > openBracket) {
            String newPath = path.substring(0, openBracket) + path.substring(closeBracket + 1);
//            System.out.println("Further cleansing path from "+path+" with "+newPath);
            return cleanPath(newPath);
        } else {
//            System.out.println("No more cleansing of path. Returning "+path);
            return path;
        }
    }

    /**
     * Assert that there is a case file event within this set of events matching the case file item path and filter.
     * @param path
     * @param filter
     * @return
     */
    public CaseFileEvent assertCaseFileEvent(String path, EventFilter<CaseFileEvent> filter) {
//        logger.debug("Searching for case file event on path "+path);
        return assertEvent("CaseFileEvent-"+path, CaseFileEvent.class, e -> {
            CaseFileEvent event = (CaseFileEvent) e;
            boolean pathMatches = path.equals(event.getPath()) || path.matches(cleanPath(event.getPath()));
            return (! (!pathMatches || !filter.matches(event)));
        });
    }

    /**
     * Assert that there are no case file events inside this set of events that match the case file item path
     * @param path CaseFileItem path, e.g. /Root/Top/List[3]
     * @return
     */
    public PublishedEventsAssertion assertNoCaseFileEvent(String path) {
        return assertNoCaseFileEvent(path, e -> true);
    }

    /**
     * Assert that there are no CaseFileEvent inside this set of events matching the case file item path
     * and the additional filter
     * @param path CaseFileItem path, e.g. /Root/Top/List[3]
     * @param filter
     * @return
     */
    public PublishedEventsAssertion assertNoCaseFileEvent(String path, EventFilter<CaseFileEvent> filter) {
        filter(CaseFileEvent.class).getEvents().forEach(event -> {
            boolean pathMatches = path.equals(event.getPath()) || path.matches(cleanPath(event.getPath()));
            if (pathMatches && filter.matches(event)) {
                throw new AssertionError("Did not expect to find a matching case file event on path "+path+", but found\n" + event);
            }
        });
        return this;
    }

    /**
     * Assert there are a certain number of events of the expected eventClass.
     * @param eventClass
     * @param expectedNumberOfEvents
     * @return
     */
    public PublishedEventsAssertion assertEventType(Class eventClass, int expectedNumberOfEvents) {
        int filteredEventsSize = filter(eventClass).getEvents().size();
        if (filteredEventsSize != expectedNumberOfEvents) {
            throw new AssertionError("Expecting " + expectedNumberOfEvents + " events of type " + eventClass.getSimpleName() + ", but " + filteredEventsSize + " events found");
        }

        return this;
    }

    /**
     * Returns a new set of events, filtered on the case instance id.
     *
     * @param caseInstanceId
     * @return
     */
    public PublishedEventsAssertion filter(String caseInstanceId) {
        return new PublishedEventsAssertion(events.stream().filter(e -> e.getActorId().equals(caseInstanceId)).collect(Collectors.toList()));
    }

    /**
     * Returns a new set of events, filtered on the class.
     * @param <T> Note, if T does not extend E the result will (obviously) be empty.
     * @param eventClass
     * @return
     */
    public <T extends ModelEvent> PublishedEventsAssertion<T> filter(Class<T> eventClass) {
        return new PublishedEventsAssertion(events.stream().filter(e -> eventClass.isAssignableFrom(e.getClass())).collect(Collectors.toList()));
    }

    /**
     * Returns a new set of events based on the filter.
     * @param <T>
     * @param filter
     * @return
     */
    public <T extends E> PublishedEventsAssertion<T> filter(EventFilter<T> filter) {
        return new PublishedEventsAssertion(events.stream().filter(e -> filter.matches((T)e)).collect(Collectors.toList()));
    }

    /**
     * Returns a new set of events that has happened between lastModified and it's previous CaseModified event, if any.
     *
     * @param lastModified
     * @return
     */
    public PublishedEventsAssertion filter(Instant lastModified) {
        List<ModelEvent> eventsSince = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            ModelEvent e = events.get(i);
            eventsSince.add(e);
            if (e instanceof CaseModified) {
                CaseModified caseModified = (CaseModified) e;
                if (caseModified.lastModified().equals(lastModified)) {
                    // Bingo, this is the event we needed.
                    return new PublishedEventsAssertion(eventsSince);
                } else {
                    // Clear the current list, as these are the events from a previous lastModified moment
                    eventsSince = new ArrayList<>();
                }
            }
        }
        ;
        return new PublishedEventsAssertion(eventsSince);
    }

    /**
     * Returns the list of events within this assertion.
     *
     * @return
     */
    public List<E> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        return events.toString();
    }
}
