package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.instance.sentry.PlanItemOnPart;
import org.cafienne.cmmn.instance.sentry.TransitionPublisher;

class PlanItemTransitionPublisher extends TransitionPublisher<PlanItemTransitioned, PlanItem<?>, PlanItemOnPart> {
    PlanItemTransitionPublisher(PlanItem<?> item) {
        super(item);
    }
}
