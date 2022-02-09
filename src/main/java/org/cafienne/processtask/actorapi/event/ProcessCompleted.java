package org.cafienne.processtask.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessCompleted extends ProcessEnded {
    public ProcessCompleted(ProcessTaskActor actor, ValueMap outputParameters) {
        super(actor, outputParameters);
    }

    public ProcessCompleted(ValueMap json) {
        super(json);
    }
}
