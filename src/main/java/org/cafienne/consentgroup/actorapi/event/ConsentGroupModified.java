package org.cafienne.consentgroup.actorapi.event;

import org.cafienne.actormodel.event.ActorModified;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event that is published after an {@link org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand} has been fully handled by a {@link ConsentGroupActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class ConsentGroupModified extends ActorModified implements ConsentGroupEvent  {
    public ConsentGroupModified(ConsentGroupActor actor, IncomingActorMessage source) {
        super(actor, source);
    }

    public ConsentGroupModified(ValueMap json) {
        super(json);
    }
}
