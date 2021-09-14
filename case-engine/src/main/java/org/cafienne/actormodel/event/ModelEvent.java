/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.event;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.TenantUserMessage;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.json.ValueMap;

public interface ModelEvent<M extends ModelActor<?,?>> extends CafienneSerializable, TenantUserMessage {
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
