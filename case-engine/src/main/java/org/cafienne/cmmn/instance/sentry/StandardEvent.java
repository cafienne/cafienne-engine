package org.cafienne.cmmn.instance.sentry;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.instance.Case;

public interface StandardEvent<T extends Enum<?>, C extends TransitionGenerator<?>> extends ModelEvent<Case> {
    T getTransition();

    C getSource();

    /**
     * Return true if, after the event has been added, and it's updateState method is invoked,
     * additional behavior must be executed (potentially leading to new events).
     * Such behavior is executed in two phases: first phase immediately after adding the event,
     * second phase only after immediate behaviors of newly generated events is also done.
     * Note that behavior is not executed upon recovery of an actor.
     */
    default boolean hasBehavior() { return false; }

    /**
     * Implement this to run behavior immediately after event is created and updateState is invoked.
     */
    default void runImmediateBehavior() {}

    /**
     * Behavior to run immediately after event is created and updateState is invoked.
     */
    void runDelayedBehavior();
}
