package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.event.ModelEvent;

public interface ProcessInstanceEvent extends ModelEvent {
    String TAG = "cafienne:process";
}
