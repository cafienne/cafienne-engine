package org.cafienne.actormodel.communication.reply.event;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ActorRequestExecuted extends ModelActorRequestEvent {
    public ActorRequestExecuted(ModelCommand command, String sourceActorId) {
        super(command, sourceActorId);
    }

    public ActorRequestExecuted(ValueMap json) {
        super(json);
    }
}
