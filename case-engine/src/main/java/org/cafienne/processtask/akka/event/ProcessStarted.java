package org.cafienne.processtask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ProcessStarted extends ProcessInstanceEvent {
    private final ValueMap input;

    private enum Fields {
        input
    }

    public ProcessStarted(ProcessTaskActor actor, ValueMap input) {
        super(actor);
        this.input = input;
    }

    public ProcessStarted(ValueMap json) {
        super(json);
        this.input = readMap(json, Fields.input);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.input, input);
    }
}
