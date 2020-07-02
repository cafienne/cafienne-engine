package org.cafienne.cmmn.akka.event.plan.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class TimerSet extends CaseEvent {
    private final static Logger logger = LoggerFactory.getLogger(TimerSet.class);

    private final Instant targetMoment;
    private final String timerId;
    private transient TimerEvent timerEvent;

    public TimerSet(TimerEvent timerEvent) {
        super(timerEvent.getCaseInstance());
        this.timerEvent = timerEvent;
        this.timerId = timerEvent.getId();
        this.targetMoment = timerEvent.getDefinition().getMoment(timerEvent);
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
    public void updateState(Case actor) {
        if (timerEvent == null) {
            timerEvent = actor.getPlanItemById(getTimerId());
            if (timerEvent == null) {
                logger.error("MAJOR ERROR: Cannot recover task timerEvent for task with id " + getTimerId() + ", because the plan item cannot be found");
                return;
            }
        }
        timerEvent.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.timerId, timerId);
        writeField(generator, Fields.targetMoment, targetMoment);
    }

    @Override
    public String toString() {
        return "Timer timerEvent "+getTimerId()+" has target moment " + targetMoment;
    }
}
