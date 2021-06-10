/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

import java.io.IOException;

/**
 * TenantUserEvents are generated on tenant users.
 */
public abstract class TenantUserEvent extends TenantEvent {
    public final String userId;

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
        User user = actor.getUser(userId);
        if (user == null) {
            logger.error("Ignoring event of type " + getClass().getName() + ", because user with id " + userId + " does not exist in tenant " + actor.getId());
        } else {
            updateUserState(actor.getUser(userId));
        }
    }

    @Override
    public String getDescription() {
        return super.getDescription() +" on user " + userId;
    }

    protected abstract void updateUserState(User user);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}
