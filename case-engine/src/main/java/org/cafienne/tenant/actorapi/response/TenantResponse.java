package org.cafienne.tenant.actorapi.response;

import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.actorapi.command.TenantCommand;

@Manifest
public class TenantResponse extends ModelResponse {
    public TenantResponse(TenantCommand command) {
        super(command);
    }

    public TenantResponse(ValueMap json) {
        super(json);
    }
}
