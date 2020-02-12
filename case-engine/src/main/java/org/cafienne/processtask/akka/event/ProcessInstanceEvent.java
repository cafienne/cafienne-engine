package org.cafienne.processtask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

public abstract class ProcessInstanceEvent extends ModelEvent<ProcessTaskActor> {
    public static final String TAG = "cafienne:process";

    protected ProcessInstanceEvent(ProcessTaskActor processInstance) {
        super(processInstance);
    }

    protected ProcessInstanceEvent(ValueMap json) {
        super(json);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }
}
