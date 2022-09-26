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

    public TimerBaseEvent(TimerEvent timerEvent) {
        super(timerEvent);
    }

    public TimerBaseEvent(ValueMap json) {
        super(json);
    }

    public String getTimerId() {
        return getPlanItemId();
    }

    @Override
    public void updateState(Case actor) {
        // Nothing to update. Avoid parent class update that searches for unnecessary plan item
    }

    @Override
    public String getPlanItemId() {
        // Unfortunately need to override this, because recovery uses the plan item id,
        // and older versions of TimerBaseEvent wrote timerId and not plan item id.
        // So we check for plan item id first (the new format) and if not present, rely on the older format.
        String planItemId = super.getPlanItemId();
        if (planItemId == null) {
            return rawJson().readString(Fields.timerId);
        } else {
            return planItemId;
        }
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        // Nothing to update.
    }

    protected void writeTimerEvent(JsonGenerator generator) throws IOException {
        super.writeCasePlanEvent(generator);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeTimerEvent(generator);
    }
}
