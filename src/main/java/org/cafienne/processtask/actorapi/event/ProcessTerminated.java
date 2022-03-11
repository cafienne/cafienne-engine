package org.cafienne.processtask.actorapi.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessTerminated extends ProcessEnded {
    public ProcessTerminated(ProcessTaskActor actor) {
        super(actor, new ValueMap());
    }

    public ProcessTerminated(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }
}
