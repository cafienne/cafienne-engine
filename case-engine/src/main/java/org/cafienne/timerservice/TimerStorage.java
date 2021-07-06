package org.cafienne.timerservice;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.actormodel.snapshot.ModelActorSnapshot;
import org.cafienne.json.ValueMap;

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
    private final Map<String, TimerJob> timers = new HashMap<>();

    public TimerStorage() {
        throw new RuntimeException("This structure is no longer supported");
    }

    public TimerStorage(ValueMap json) {
        json.withArray(Fields.timers).forEach(value -> addTimer(new TimerJob(value.asMap())));
    }

    public Collection<TimerJob> getTimers() {
        return new ArrayList<>(timers.values());
    }

    private void addTimer(TimerJob job) {
        timers.put(job.timerId, job);
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
