package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessResumed extends ProcessInstanceEvent {
    public ProcessResumed(ProcessTaskActor actor) {
        super(actor);
    }

    public ProcessResumed(ValueMap json) {
        super(json);
    }
}
