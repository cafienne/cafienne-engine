package org.cafienne.processtask.akka.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessFailed extends ProcessEnded {
    public ProcessFailed(ProcessTaskActor actor, ValueMap outputParameters) {
        super(actor, outputParameters);
    }

    public ProcessFailed(ValueMap json) {
        super(json);
    }
}
