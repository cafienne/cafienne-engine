/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.event;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.message.UserMessage;
import org.cafienne.json.ValueMap;

import java.time.Instant;
import java.util.Set;

public interface ModelEvent extends UserMessage {
    String TAG = "cafienne";

    Set<String> tags = Set.of(ModelEvent.TAG);

    default Set<String> tags() {
        return tags;
    }

    void updateActorState(ModelActor actor);

    default String getTenant() {
        return tenant();
    }

    String tenant();

    String getActorId();

    Instant getTimestamp();

    String getDescription();

    ValueMap rawJson();
}
