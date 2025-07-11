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

package org.cafienne.engine.cmmn.test.assertions.event;

import org.cafienne.engine.cmmn.actorapi.event.CaseEvent;

/**
 * Basic assertions around case instance events.
 * @param <T>
 */
public class CaseInstanceEventAssertion<T extends CaseEvent> {
    protected final T event;
    protected CaseInstanceEventAssertion(T event) {
        this.event = event;
    }

    /**
     * Asserts the case instance id of the event
     * @param caseInstanceId
     * @return
     */
    public CaseInstanceEventAssertion<T> assertCaseId(String caseInstanceId) {
        if (!event.getCaseInstanceId().equals(caseInstanceId)) {
            throw new AssertionError("This event is expected to be for case "+caseInstanceId+", but it is for case "+event.getCaseInstanceId());
        }
        return this;
    }
}
