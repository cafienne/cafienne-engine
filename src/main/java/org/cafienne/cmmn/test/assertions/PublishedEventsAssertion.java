package org.cafienne.cmmn.test.assertions;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.actorapi.event.CaseModified;
import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned;
import org.cafienne.cmmn.instance.Path;
import org.cafienne.cmmn.test.CaseTestCommand;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.filter.EventFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Set of assertions around the events that have been published after the test script has handled a command.
 * Events are taken from {@link CaseTestCommand#getEventListener()}.
 */
public class PublishedEventsAssertion<E extends ModelEvent> {
    private final List<E> events;

    public PublishedEventsAssertion(List<E> publishedEvents) {
        this.events = publishedEvents;
    }

    /**
     * Asserts the number of events that have been published
     *
     * @param expectedNumberOfEvents
     * @return
     */
    public PublishedEventsAssertion<E> assertSize(int expectedNumberOfEvents) {
        if (events.size() != expectedNumberOfEvents) {
            String types = events.stream().map(e -> e.getClass().getSimpleName()).collect(Collectors.toSet()).toString();
            throw new AssertionError("Expecting " + expectedNumberOfEvents + ", but found " + events.size() + " events: " + types);
        }
        return this;
    }

    /**
     * Assert the set of events is not empty
     * @return
     */
    public PublishedEventsAssertion<E> assertNotEmpty() {
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
        for (ModelEvent event : events) {
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
    public CaseFileItemTransitioned assertCaseFileEvent(Path path, EventFilter<CaseFileItemTransitioned> filter) {
//        logger.debug("Searching for case file event on path "+path);
        return assertEvent("CaseFileItemTransitioned-"+path, CaseFileItemTransitioned.class, event -> path.matches(event.getPath()) && filter.matches(event));
    }

    /**
     * Assert that there are no case file events inside this set of events that match the case file item path
     * @param path CaseFileItem path, e.g. /Root/Top/List[3]
     * @return
     */
    public PublishedEventsAssertion<E> assertNoCaseFileEvent(Path path) {
        return assertNoCaseFileEvent(path, e -> true);
    }

    /**
     * Assert that there are no CaseFileItemTransitioned inside this set of events matching the case file item path
     * and the additional filter
     * @param path CaseFileItem path, e.g. /Root/Top/List[3]
     * @param filter
     * @return
     */
    public PublishedEventsAssertion<E> assertNoCaseFileEvent(Path path, EventFilter<CaseFileItemTransitioned> filter) {
        filter(CaseFileItemTransitioned.class).getEvents().forEach(event -> {
            if (path.matches(event.getPath()) && filter.matches(event)) {
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
    public <M extends ModelEvent> PublishedEventsAssertion<E> assertEventType(Class<M> eventClass, int expectedNumberOfEvents) {
        int filteredEventsSize = filter(eventClass).getEvents().size();
        if (filteredEventsSize != expectedNumberOfEvents) {
            throw new AssertionError("Expecting " + expectedNumberOfEvents + " events of type " + eventClass.getSimpleName() + ", but " + filteredEventsSize + " events found");
        }

        return this;
    }

    public String enumerateEventsByType() {
        Map<Class<?>, Integer> eventsByType = new LinkedHashMap<>();
        events.forEach(event -> eventsByType.put(event.getClass(), (eventsByType.getOrDefault(event.getClass(), 0) + 1)));
        StringBuilder sb = new StringBuilder();
        eventsByType.forEach((eventClass, number) -> sb.append(eventClass.getSimpleName() +": " + number + "\n"));
        return sb.toString();
    }

    public void printSummary() {
        TestScript.debugMessage("Found these events:\n"  + enumerateEventsByType());
    }

    public void printEventList() {
        StringBuffer eventList = new StringBuffer("\nEvents:");
        events.forEach(event -> eventList.append("\n " + (events.indexOf(event)) +": " + event ));
        TestScript.debugMessage(eventList+"\n");
    }

    /**
     * Returns a new set of events, filtered on the case instance id.
     *
     * @param caseInstanceId
     * @return
     */
    public PublishedEventsAssertion<E> filter(String caseInstanceId) {
        return new PublishedEventsAssertion<>(events.stream().filter(e -> e.getActorId().equals(caseInstanceId)).collect(Collectors.toList()));
    }

    /**
     * Returns a new set of events, filtered on the class.
     * @param <T> Note, if T does not extend E the result will (obviously) be empty.
     * @param eventClass
     * @return
     */
    public <T extends ModelEvent> PublishedEventsAssertion<T> filter(Class<T> eventClass) {
        return new PublishedEventsAssertion<>(events.stream().filter(e -> eventClass.isAssignableFrom(e.getClass())).map(e -> (T)e).collect(Collectors.toList()));
    }

    /**
     * Returns a new set of events based on the filter.
     * @param <T>
     * @param filter
     * @return
     */
    public <T extends E> PublishedEventsAssertion<T> filter(EventFilter<T> filter) {
        return new PublishedEventsAssertion<>(events.stream().map(e -> (T)e).filter(filter::matches).collect(Collectors.toList()));
    }

    /**
     * Returns a new set of events that has happened between lastModified and it's previous CaseModified event, if any.
     *
     * @param lastModified
     * @return
     */
    public PublishedEventsAssertion<E> filter(Instant lastModified) {
        List<E> eventsSince = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            E e = events.get(i);
            eventsSince.add(e);
            if (e instanceof CaseModified) {
                CaseModified caseModified = (CaseModified) e;
                if (caseModified.lastModified().equals(lastModified)) {
                    // Bingo, this is the event we needed.
                    return new PublishedEventsAssertion<>(eventsSince);
                } else {
                    // Clear the current list, as these are the events from a previous lastModified moment
                    eventsSince = new ArrayList<>();
                }
            }
        }
        ;
        return new PublishedEventsAssertion<>(eventsSince);
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
