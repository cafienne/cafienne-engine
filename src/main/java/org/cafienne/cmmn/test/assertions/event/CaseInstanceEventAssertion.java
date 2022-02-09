package org.cafienne.cmmn.test.assertions.event;

import org.cafienne.cmmn.actorapi.event.CaseEvent;

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
