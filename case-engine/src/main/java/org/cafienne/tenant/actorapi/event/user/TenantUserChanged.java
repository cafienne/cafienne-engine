package org.cafienne.tenant.actorapi.event.user;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;
import java.util.Set;

@Manifest
public class TenantUserChanged extends TenantMemberEvent {
    public final Set<String> rolesRemoved;

    public TenantUserChanged(TenantActor tenant, TenantUser user, Set<String> rolesRemoved) {
        super(tenant, user);
        this.rolesRemoved = rolesRemoved;
    }

    public TenantUserChanged(ValueMap json) {
        super(json);
        this.rolesRemoved = json.readSet(Fields.rolesRemoved);
    }

    @Override
    public void updateState(TenantActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTenantUserEvent(generator);
        writeField(generator, Fields.rolesRemoved, rolesRemoved);
    }
}
