package org.cafienne.tenant.actorapi.event.deprecated;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantUserDisabled extends DeprecatedTenantUserEvent {

    public TenantUserDisabled(TenantActor tenant, String userId) {
        super(tenant, userId);
    }

    public TenantUserDisabled(ValueMap json) {
        super(json);
    }
}
