package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class TimerDropped extends TimerCleared {
    public TimerDropped(TimerEvent timerEvent) {
        super(timerEvent);
    }

    public TimerDropped(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" is dropped during migration";
    }
}
