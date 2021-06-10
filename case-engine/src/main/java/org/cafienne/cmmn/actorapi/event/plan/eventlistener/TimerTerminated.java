package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.TimerEvent;

@Manifest
public class TimerTerminated extends TimerCleared {
    public TimerTerminated(TimerEvent timerEvent) {
        super(timerEvent);
    }

    public TimerTerminated(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" is terminated";
    }
}
