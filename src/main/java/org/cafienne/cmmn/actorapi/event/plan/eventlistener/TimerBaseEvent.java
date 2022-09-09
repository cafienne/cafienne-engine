package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.actorapi.event.plan.CasePlanEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Set;

/**
 * Base class for all TimerEventListener based akka events.
 * These events are tagged so that a projection can handle them and safely set timers.
 */
public abstract class TimerBaseEvent extends CasePlanEvent {
    public static final String TAG = "cafienne:timer";

    private static final Set<String> tags = Set.of(ModelEvent.TAG, CaseEvent.TAG, TimerBaseEvent.TAG);

    @Override
    public Set<String> tags() {
        return tags;
    }

    private final String timerId;

    public TimerBaseEvent(TimerEvent timerEvent) {
        super(timerEvent);
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
        // Nothing to update. Avoid parent class update that searches for unnecessary plan item
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        // Nothing to update.
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCasePlanEvent(generator);
        writeField(generator, Fields.timerId, timerId);
    }

    @Override
    public String toString() {
        return "Timer " + getTimerId() + " has completed";
    }
}
