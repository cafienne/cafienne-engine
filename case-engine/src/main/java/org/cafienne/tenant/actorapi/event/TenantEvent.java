/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.BaseModelEvent;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

/**
 * TenantEvents are generated by the {@link TenantActor}.
 */
public abstract class TenantEvent extends BaseModelEvent<TenantActor> {
    public static final String TAG = "cafienne:tenant";

    protected TenantEvent(TenantActor tenant) {
        super(tenant);
    }

    protected TenantEvent(ValueMap json) {
        super(json);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }
}
