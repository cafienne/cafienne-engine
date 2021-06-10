package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.instance.TimerEvent;

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
