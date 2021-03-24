package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.instance.TimerEvent;

/**
 */
public class TimerEventAPI extends PlanItemAPI<TimerEvent> {
    TimerEventAPI(TimerEvent timerEvent, StageAPI stage) {
        super(timerEvent, stage);
    }

    @Override
    public String getName() {
        return "timer";
    }
}
