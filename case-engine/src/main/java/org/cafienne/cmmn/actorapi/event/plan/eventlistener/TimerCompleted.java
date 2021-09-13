package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class TimerCompleted extends TimerCleared {
    public TimerCompleted(TimerEvent timerEvent) {
        super(timerEvent);
    }

    public TimerCompleted(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" has completed";
    }
}
