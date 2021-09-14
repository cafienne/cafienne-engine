package org.cafienne.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.cmmn.actorapi.command.platform.TenantUpdate;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;

@Manifest
public class UpdateTenantWithPlatformInformation extends PlatformTenantCommand {
    private final PlatformUpdate newUserInformation;

    public UpdateTenantWithPlatformInformation(PlatformUser user, TenantUpdate action) {
        super(user, action.tenant());
        this.newUserInformation = action.platformUpdate();
    }

    public UpdateTenantWithPlatformInformation(ValueMap json) {
        super(json);
        newUserInformation = PlatformUpdate.deserialize(json.withArray(Fields.users));
    }

    @Override
    public void done() { // Avoid adding a TenantModified
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.updatePlatformInformation(this.newUserInformation);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.users, newUserInformation.toValue());
    }
}

