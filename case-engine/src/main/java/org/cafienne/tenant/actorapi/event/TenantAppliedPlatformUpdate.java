package org.cafienne.tenant.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

@Manifest
public class TenantAppliedPlatformUpdate extends TenantEvent {
    public final PlatformUpdate newUserInformation;

    public TenantAppliedPlatformUpdate(TenantActor tenant, PlatformUpdate newUserInformation) {
        super(tenant);
        this.newUserInformation = newUserInformation;
    }

    public TenantAppliedPlatformUpdate(ValueMap json) {
        super(json);
        newUserInformation = PlatformUpdate.deserialize(json.withArray(Fields.users));
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " on " + newUserInformation.info().size() +" users";
    }

    @Override
    public void updateState(TenantActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.users, newUserInformation.toValue());
    }
}
