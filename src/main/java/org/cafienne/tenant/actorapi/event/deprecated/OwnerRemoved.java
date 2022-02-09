package org.cafienne.tenant.actorapi.event.deprecated;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class OwnerRemoved extends DeprecatedTenantUserEvent {
    public OwnerRemoved(TenantActor tenant, String userId) {
        super(tenant, userId);
    }

    public OwnerRemoved(ValueMap json) {
        super(json);
    }
}
