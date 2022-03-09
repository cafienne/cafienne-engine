package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Set;

/**
 * Base class for all TimerEventListener based akka events.
 * These events are tagged so that a projection can handle them and safely set timers.
 */
public abstract class TimerBaseEvent extends CaseBaseEvent {
    public static final String TAG = "cafienne:timer";

    private static Set<String> tags = Set.of(ModelEvent.TAG, CaseEvent.TAG, TimerBaseEvent.TAG);

    @Override
    public Set<String> tags() {
        return tags;
    }

    private final String timerId;

    public TimerBaseEvent(TimerEvent timerEvent) {
        super(timerEvent.getCaseInstance());
        this.timerId = timerEvent.getId();
    }

    public TimerBaseEvent(ValueMap json) {
        super(json);
        this.timerId = json.readString(Fields.timerId);
    }

    public String getTimerId() {
        return timerId;
    }

    @Override
    public void updateState(Case actor) {
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.timerId, timerId);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" has completed";
    }
}
