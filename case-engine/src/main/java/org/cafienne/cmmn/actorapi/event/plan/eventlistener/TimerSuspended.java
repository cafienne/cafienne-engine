package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class TimerSuspended extends TimerCleared {
    public TimerSuspended(TimerEvent timerEvent) {
        super(timerEvent);
    }

    public TimerSuspended(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" is suspended";
    }
}
