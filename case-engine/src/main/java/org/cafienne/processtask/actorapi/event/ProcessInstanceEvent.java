package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.processtask.actorapi.ProcessActorMessage;
import org.cafienne.processtask.instance.ProcessTaskActor;

public interface ProcessInstanceEvent extends ModelEvent, ProcessActorMessage {
    String TAG = "cafienne:process";

    @Override
    default Class<ProcessTaskActor> actorClass() {
        return ProcessTaskActor.class;
    }
}
