package org.cafienne.processtask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

public abstract class ProcessEnded extends ProcessInstanceEvent {

    public final ValueMap output;

    protected ProcessEnded(ProcessTaskActor actor, ValueMap outputParameters) {
        super(actor);
        this.output = outputParameters;
    }

    protected ProcessEnded(ValueMap json) {
        super(json);
        this.output = readMap(json, Fields.output);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.output, output);
    }
}
