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

package org.cafienne.cmmn.test.filter;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.test.CaseEventListener;

/**
 * An EventFilter can be used to wait until a certain event (or combination of events)
 * has been published on the event stream that comes out of the actor system and is captured in
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

