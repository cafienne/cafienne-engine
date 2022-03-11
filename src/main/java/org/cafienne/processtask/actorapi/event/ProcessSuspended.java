package org.cafienne.processtask.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessSuspended extends BaseProcessEvent {
    public ProcessSuspended(ProcessTaskActor actor) {
        super(actor);
    }

    public ProcessSuspended(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }
}
