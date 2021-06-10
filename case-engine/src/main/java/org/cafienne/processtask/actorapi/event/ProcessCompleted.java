package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.actormodel.serialization.json.ValueMap;
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
