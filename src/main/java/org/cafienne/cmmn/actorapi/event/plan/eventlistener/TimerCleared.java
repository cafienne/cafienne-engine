package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.json.ValueMap;

/**
 * Base class when the case no longer needs the timer to "actively" run.
 * Either due to completion or due to suspension.
 */
public abstract class TimerCleared extends TimerBaseEvent {
    protected TimerCleared(TimerEvent timerEvent) {
        super(timerEvent);
    }

    protected TimerCleared(ValueMap json) {
        super(json);
    }
}
