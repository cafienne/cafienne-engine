/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.actorapi.CaseMessage;
import org.cafienne.cmmn.instance.Case;

import java.util.Set;

public interface CaseEvent extends ModelEvent, CaseMessage {
    String TAG = "cafienne:case";

    Set<String> tags = Set.of(ModelEvent.TAG, CaseEvent.TAG);

    @Override
    default Set<String> tags() {
        return tags;
    }

    default String getCaseInstanceId() {
        return this.getActorId();
    }

    @Override
    default Class<Case> actorClass() {
        return Case.class;
    }
}
