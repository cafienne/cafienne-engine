package org.cafienne.actormodel.event;

import org.cafienne.actormodel.ModelActor;

import java.time.Instant;

/**
 * Interface for a ModelActor to use to express that an event completes the handling of a certain
 * incoming message that has lead to state changes in the actor.
 *
 * @param <M>
 */
public interface TransactionEvent<M extends ModelActor<?,?>> extends ModelEvent<M> {
    Instant lastModified();

    /**
     * Cause is set directly after the event is created. Is typically invoked with the
     * class name of the message that caused the actor to change state.
     * @param source
     */
    void setCause(String source);
}
