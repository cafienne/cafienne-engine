package org.cafienne.processtask.actorapi;

import org.cafienne.actormodel.message.UserMessage;
import org.cafienne.processtask.instance.ProcessTaskActor;

public interface ProcessActorMessage extends UserMessage {
    @Override
    default Class<ProcessTaskActor> actorClass() {
        return ProcessTaskActor.class;
    }
}
