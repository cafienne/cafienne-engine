package org.cafienne.tenant.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.platform.NewUserInformation;
import org.cafienne.cmmn.akka.command.platform.PlatformUpdate;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public void updateState(TenantActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.users, newUserInformation.toValue());
    }
}
