/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.instance.sentry;

import com.casefabric.cmmn.actorapi.event.CaseEvent;

public interface StandardEvent<T extends Enum<?>, C extends TransitionGenerator<?>> extends CaseEvent {
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
