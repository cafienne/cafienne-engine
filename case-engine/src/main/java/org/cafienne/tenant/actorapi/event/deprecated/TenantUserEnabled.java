package org.cafienne.tenant.actorapi.event.deprecated;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantUserEnabled extends DeprecatedTenantUserEvent {
    public TenantUserEnabled(TenantActor tenant, String userId) {
        super(tenant, userId);
    }

    public TenantUserEnabled(ValueMap json) {
        super(json);
    }
}
