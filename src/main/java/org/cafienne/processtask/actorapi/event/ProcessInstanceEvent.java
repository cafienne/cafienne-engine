package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.processtask.actorapi.ProcessActorMessage;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.util.Set;

public interface ProcessInstanceEvent extends ModelEvent, ProcessActorMessage {
    String TAG = "cafienne:process";

    Set<String> tags = Set.of(ModelEvent.TAG, ProcessInstanceEvent.TAG);

    @Override
    default Set<String> tags() {
        return tags;
    }
}
