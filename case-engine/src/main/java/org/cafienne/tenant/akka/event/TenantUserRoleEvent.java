/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

/**
 * TenantUserRoleEvents are generated on role changes of tenant users
 */
public abstract class TenantUserRoleEvent extends TenantUserEvent {

    public final String role;

    protected TenantUserRoleEvent(TenantActor tenant, String userId, String role) {
        super(tenant, userId);
        this.role = role;
    }

    protected TenantUserRoleEvent(ValueMap json) {
        super(json);
        this.role = readField(json, Fields.role);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, role);
    }
}
