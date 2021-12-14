package org.cafienne.tenant.actorapi.event.deprecated;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class OwnerAdded extends DeprecatedTenantUserEvent {
    public OwnerAdded(TenantActor tenant, String userId) {
        super(tenant, userId);
    }

    public OwnerAdded(ValueMap json) {
        super(json);
    }
}
