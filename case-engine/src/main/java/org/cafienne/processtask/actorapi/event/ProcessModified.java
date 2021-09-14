package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.event.ActorModified;
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
public class ProcessModified extends ActorModified<ProcessTaskActor> implements ProcessInstanceEvent {

    public ProcessModified(ProcessCommand command, ProcessTaskActor actor, Instant lastModified) {
        super(command);
    }

    public ProcessModified(ValueMap json) {
        super(json);
    }
}
