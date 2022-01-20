package org.cafienne.tenant.actorapi.response;

import org.cafienne.actormodel.response.BaseModelResponse;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.actorapi.command.TenantCommand;

@Manifest
public class  TenantResponse extends BaseModelResponse {
    public TenantResponse(TenantCommand command) {
        super(command);
    }

    public TenantResponse(ValueMap json) {
        super(json);
    }
}
