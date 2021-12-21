/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.actorapi.event.deprecated;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.event.TenantBaseEvent;

import java.io.IOException;

/**
 * TenantUserEvents are generated on tenant users.
 */
public abstract class DeprecatedTenantUserEvent extends TenantBaseEvent {
    public final String userId;

    protected DeprecatedTenantUserEvent(TenantActor tenant, String userId) {
        super(tenant);
        this.userId = userId;
    }

    protected DeprecatedTenantUserEvent(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
    }

    @Override
    public void updateState(TenantActor actor) {
        actor.updateState(this);
    }

    @Override
    public String getDescription() {
        return super.getDescription() +" on user " + userId;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTenantEvent(generator);
        writeField(generator, Fields.userId, userId);
    }
}
