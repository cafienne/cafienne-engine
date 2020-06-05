package org.cafienne.timerservice;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.snapshot.ModelActorSnapshot;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Object that can be saved as snapshot offer for the TimerService persistent actor
 */
@Manifest
public class TimerStorage implements ModelActorSnapshot {
    private Map<String, TimerJob> timers = new HashMap();

    private enum Fields {
        timers, timerId, caseInstanceId, moment, user
    }

    public TimerStorage() {
    }

    public TimerStorage(ValueMap json) {
        json.withArray(Fields.timers).forEach(value -> addTimer(new TimerJob((ValueMap) value)));
    }

    Collection<TimerJob> getTimers() {
        return new ArrayList<>(timers.values());
    }

    void addTimer(TimerJob job) {
        timers.put(job.timerId, job);
    }

    void removeTimer(String timerId) {
        timers.remove(timerId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart(Fields.timers.toString());
        for (TimerJob timer: getTimers()) {
            timer.writeThisObject(generator);
        }
        generator.writeEndArray();
    }
}
