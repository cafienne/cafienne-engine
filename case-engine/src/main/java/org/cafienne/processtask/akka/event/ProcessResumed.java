package org.cafienne.processtask.akka.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessResumed extends ProcessInstanceEvent {
    public ProcessResumed(ProcessTaskActor actor) {
        super(actor);
    }

    public ProcessResumed(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        // Nothing to update
    }

    @Override
    public void runImmediateBehavior() {
        actor.resume();
    }
}
