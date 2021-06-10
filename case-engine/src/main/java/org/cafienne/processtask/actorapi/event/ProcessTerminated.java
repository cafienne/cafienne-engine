package org.cafienne.processtask.actorapi.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessTerminated extends ProcessEnded {
    public ProcessTerminated(ProcessTaskActor actor) {
        super(actor, new ValueMap());
    }

    public ProcessTerminated(ValueMap json) {
        super(json);
    }
}
