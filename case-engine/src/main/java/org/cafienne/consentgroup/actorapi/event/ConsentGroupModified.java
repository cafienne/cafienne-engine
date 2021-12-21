package org.cafienne.consentgroup.actorapi.event;

import org.cafienne.actormodel.event.ActorModified;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event that is published after an {@link org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand} has been fully handled by a {@link ConsentGroupActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class ConsentGroupModified extends ActorModified<ConsentGroupActor> implements ConsentGroupEvent  {
    public ConsentGroupModified(ConsentGroupCommand command) {
        super(command);
    }

    public ConsentGroupModified(ValueMap json) {
        super(json);
    }
}
