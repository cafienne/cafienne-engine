package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.event.ActorModified;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.ProcessCommand;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.time.Instant;

/**
 * Event that is published after an {@link org.cafienne.processtask.actorapi.command.ProcessCommand} has been fully handled by a {@link ProcessTaskActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class ProcessModified extends ActorModified implements ProcessInstanceEvent {

    public ProcessModified(ProcessTaskActor actor, IncomingActorMessage source) {
        super(actor, source);
    }

    public ProcessModified(ValueMap json) {
        super(json);
    }
}
