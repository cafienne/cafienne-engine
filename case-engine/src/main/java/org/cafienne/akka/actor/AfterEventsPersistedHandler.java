package org.cafienne.akka.actor;

@FunctionalInterface
public interface AfterEventsPersistedHandler<T> {
    void handleAfterEventsPersisted(T e);
}
