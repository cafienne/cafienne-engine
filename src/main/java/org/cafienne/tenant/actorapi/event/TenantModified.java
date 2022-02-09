package org.cafienne.tenant.actorapi.event;

import org.cafienne.actormodel.event.ActorModified;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

/**
 * Event that is published after an {@link org.cafienne.tenant.actorapi.command.TenantCommand} has been fully handled by a {@link TenantActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class TenantModified extends ActorModified implements TenantEvent {
    public TenantModified(TenantActor actor, IncomingActorMessage source) {
        super(actor, source);
    }

    public TenantModified(ValueMap json) {
        super(json);
    }
}
