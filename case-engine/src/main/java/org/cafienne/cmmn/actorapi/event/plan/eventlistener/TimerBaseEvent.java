package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.TimerEvent;

import java.io.IOException;

/**
 * Base class for all TimerEventListener based akka events.
 * These events are tagged so that a projection can handle them and safely set timers.
 */
public abstract class TimerBaseEvent extends CaseEvent {
    public static final String TAG = "cafienne:timer";

    private final String timerId;

    public TimerBaseEvent(TimerEvent timerEvent) {
        super(timerEvent.getCaseInstance());
        this.timerId = timerEvent.getId();
    }

    public TimerBaseEvent(ValueMap json) {
        super(json);
        this.timerId = json.raw(Fields.timerId);
    }

    public String getTimerId() {
        return timerId;
    }

    @Override
    public void updateState(Case actor) {
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.timerId, timerId);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" has completed";
    }
}
