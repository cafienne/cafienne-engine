package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.processtask.instance.ProcessTaskActor;

public interface ProcessInstanceEvent extends ModelEvent<ProcessTaskActor> {
    String TAG = "cafienne:process";
}
