package org.cafienne.cmmn.test.filter;

import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.cmmn.test.CaseEventListener;

/**
 * An EventFilter can be used to wait until a certain event (or combination of events)
 * has been published on the event stream that comes out of Akka and is captured in
 * the {@link CaseEventListener}. See also {@link CaseEventListener#waitUntil(String, Class, EventFilter, long...)}
 */
@FunctionalInterface
public interface EventFilter<T extends ModelEvent> {
    /**
     * If the event matches the matches, this has to return true; false otherwise.
     *
     * @param event         The current event that has come out of the event stream
     * @return <code>true</code> if the filter matches, false otherwise. Returning false will make the event listener wait until new events have arrived.
     */
    boolean matches(T event);
}

