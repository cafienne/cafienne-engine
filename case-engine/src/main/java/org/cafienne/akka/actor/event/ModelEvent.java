/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor.event;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.TenantUserMessage;
import org.cafienne.akka.actor.serialization.CafienneSerializable;
import org.cafienne.akka.actor.serialization.json.ValueMap;

public interface ModelEvent<M extends ModelActor> extends CafienneSerializable, TenantUserMessage<M> {
    String TAG = "cafienne";

    void updateState(M actor);

    void recover(M actor);

    String getTenant();

    default String tenant() {
        return getTenant();
    }

    String getActorId();

    String getDescription();

    ValueMap rawJson();
}
