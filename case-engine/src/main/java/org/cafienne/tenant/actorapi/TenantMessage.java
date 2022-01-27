/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.actorapi;

import org.cafienne.actormodel.message.UserMessage;
import org.cafienne.tenant.TenantActor;

/**
 * TenantEvents are generated by the {@link TenantActor}.
 */
public interface TenantMessage extends UserMessage {
    @Override
    default Class<TenantActor> actorClass() {
        return TenantActor.class;
    }
}
