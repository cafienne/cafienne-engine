package org.cafienne.processtask.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessResumed extends BaseProcessEvent {
    public ProcessResumed(ProcessTaskActor actor) {
        super(actor);
    }

    public ProcessResumed(ValueMap json) {
        super(json);
    }
}
