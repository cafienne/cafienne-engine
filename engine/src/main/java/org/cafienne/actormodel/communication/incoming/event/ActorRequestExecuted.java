package org.cafienne.actormodel.communication.incoming.event;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ActorRequestExecuted extends IncomingActorRequestEvent {
    public ActorRequestExecuted(ModelCommand command, String sourceActorId) {
        super(command, sourceActorId);
    }

    public ActorRequestExecuted(ValueMap json) {
        super(json);
    }
}
