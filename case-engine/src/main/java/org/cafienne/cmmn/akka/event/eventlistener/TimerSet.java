package org.cafienne.cmmn.akka.event.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseInstanceEvent;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class TimerSet extends CaseInstanceEvent {
    private final static Logger logger = LoggerFactory.getLogger(TimerSet.class);

    private final Instant targetMoment;
    private final String timerId;

    private enum Fields {
        timerId, targetMoment
    }

    public TimerSet(TimerEvent event) {
        super(event.getCaseInstance());
        this.timerId = event.getId();
        this.targetMoment = event.getTargetMoment();
    }

    public TimerSet(ValueMap json) {
        super(json);
        this.timerId = json.raw(Fields.timerId);
        this.targetMoment = Instant.parse(json.raw(Fields.targetMoment));
    }

    public Instant getTargetMoment() {
        return targetMoment;
    }

    public String getTimerId() {
        return timerId;
    }

    @Override
    final public void recover(Case caseInstance) {
        PlanItem planItem = caseInstance.getPlanItemById(getTimerId());
        if (planItem == null) {
            logger.error("MAJOR ERROR: Cannot recover task event for task with id " + getTimerId() + ", because the plan item cannot be found");
            return;
        }
        TimerEvent timer = planItem.getInstance();
        timer.recover(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.timerId, timerId);
        writeField(generator, Fields.targetMoment, targetMoment);
    }

    @Override
    public String toString() {
        return "Timer event "+getTimerId()+" has target moment " + targetMoment;
    }
}
