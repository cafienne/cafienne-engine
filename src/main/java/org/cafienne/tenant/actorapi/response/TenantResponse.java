package org.cafienne.tenant.actorapi.response;

import org.cafienne.actormodel.response.BaseModelResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.actorapi.TenantMessage;
import org.cafienne.tenant.actorapi.command.TenantCommand;

@Manifest
public class TenantResponse extends BaseModelResponse implements TenantMessage {
    public TenantResponse(TenantCommand command) {
        super(command);
    }

    public TenantResponse(ValueMap json) {
        super(json);
    }
}
