/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

import java.io.IOException;

/**
 * TenantUserEvents are generated on tenant users.
 */
public abstract class TenantUserEvent extends TenantEvent {
    public final String userId;

    protected enum Fields {
        userId
    }

    protected TenantUserEvent(TenantActor tenant, String userId) {
        super(tenant);
        this.userId = userId;
    }

    protected TenantUserEvent(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
    }

    @Override
    public void updateState(TenantActor actor) {
        updateUserState(actor.getUser(userId));
    }

    protected abstract void updateUserState(User user);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}
