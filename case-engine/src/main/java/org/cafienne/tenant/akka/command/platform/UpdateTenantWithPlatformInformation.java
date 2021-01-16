package org.cafienne.tenant.akka.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.platform.NewUserInformation;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;

import java.io.IOException;
import java.util.List;

@Manifest
public class UpdateTenantWithPlatformInformation extends PlatformTenantCommand {
    private final List<NewUserInformation> newUserInformation;

    public UpdateTenantWithPlatformInformation(PlatformUser user, String tenantId, List<NewUserInformation> newUserInformation) {
        super(user, tenantId);
        this.newUserInformation = newUserInformation;
    }

    public UpdateTenantWithPlatformInformation(ValueMap json) {
        super(json);
        newUserInformation = NewUserInformation.deserialize(json.withArray(Fields.users));
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.updatePlatformInformation(this.newUserInformation);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeListField(generator, Fields.users, newUserInformation);
    }
}

