package org.cafienne.processtask.akka.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
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
