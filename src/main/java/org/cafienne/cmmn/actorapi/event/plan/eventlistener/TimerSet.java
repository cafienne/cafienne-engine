package org.cafienne.cmmn.actorapi.event.plan.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class TimerSet extends TimerBaseEvent {
    private final static Logger logger = LoggerFactory.getLogger(TimerSet.class);

    private final Instant targetMoment;
    private transient TimerEvent timerEvent;

    public TimerSet(TimerEvent timerEvent) {
        super(timerEvent);
        this.timerEvent = timerEvent;
        this.targetMoment = timerEvent.getDefinition().getMoment(timerEvent);
    }

    public TimerSet(ValueMap json) {
        super(json);
        this.targetMoment = json.readInstant(Fields.targetMoment);
    }

    public Instant getTargetMoment() {
        return targetMoment;
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
        super.write(generator);
        writeField(generator, Fields.targetMoment, targetMoment);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" is set to occur at " + targetMoment;
    }
}
