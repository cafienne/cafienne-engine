package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.instance.TimerEvent;

@Manifest
public class TimerResumed extends TimerSet {
    public TimerResumed(TimerEvent timerEvent) {
        super(timerEvent);
    }

    public TimerResumed(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" is resumed";
    }
}
