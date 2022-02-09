package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
import org.cafienne.cmmn.instance.TimerEvent;

/**
 * Provides context for evaluating expressions inside TimerEvents.
 * Note, these are generally considered to run only on e.g. case.file or to have a hardcoded Duration expression (e.g., Instant.now())
 */
public class TimerExpressionAPI extends CaseRootObject {
    public TimerExpressionAPI(TimerEvent timer) {
        super(timer.getCaseInstance());
    }

    @Override
    public String getDescription() {
        return "timer event duration";
    }
}
