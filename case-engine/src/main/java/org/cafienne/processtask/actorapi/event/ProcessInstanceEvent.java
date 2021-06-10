package org.cafienne.processtask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.BaseModelEvent;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

public abstract class ProcessInstanceEvent extends BaseModelEvent<ProcessTaskActor> {
    public static final String TAG = "cafienne:process";

    protected ProcessInstanceEvent(ProcessTaskActor processInstance) {
        super(processInstance);
    }

    protected ProcessInstanceEvent(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        // Nothing to update here. (as of now)
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }
}
