package org.cafienne.tenant.actorapi.event.user;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.event.TenantBaseEvent;

import java.io.IOException;

public abstract class TenantMemberEvent extends TenantBaseEvent {
    private final TenantUser user;
    public final TenantUser member;
    public final String memberId;

    protected TenantMemberEvent(TenantActor tenant, TenantUser user) {
        super(tenant);
        this.user = user;
        this.member = this.user;
        this.memberId = this.user.id();
    }

    protected TenantMemberEvent(ValueMap json) {
        super(json);
        this.user = json.readObject(Fields.user, TenantUser::deserialize);
        this.member = this.user;
        this.memberId = this.user.id();
    }

    protected void writeTenantUserEvent(JsonGenerator generator) throws IOException {
        super.writeTenantEvent(generator);
        writeField(generator, Fields.user, user);
    }
}
