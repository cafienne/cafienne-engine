package org.cafienne.processtask.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessFailed extends ProcessEnded {
    public ProcessFailed(ProcessTaskActor actor, ValueMap outputParameters) {
        super(actor, outputParameters);
    }

    public ProcessFailed(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }
}
