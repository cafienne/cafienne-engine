package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.instance.TimerEvent;

/**
 */
public class TimerEventAPI extends PlanItemAPI<TimerEvent> {
    TimerEventAPI(CaseAPI caseAPI, TimerEvent timerEvent, StageAPI stage) {
        super(caseAPI, timerEvent, stage);
    }

    @Override
    public String getName() {
        return "timer";
    }
}
