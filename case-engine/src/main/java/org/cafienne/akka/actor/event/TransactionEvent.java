package org.cafienne.akka.actor.event;

import org.cafienne.akka.actor.ModelActor;

import java.time.Instant;

public interface TransactionEvent<M extends ModelActor> extends ModelEvent<M> {
    Instant lastModified();
}
