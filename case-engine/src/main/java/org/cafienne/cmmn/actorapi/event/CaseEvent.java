/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event;

import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.cmmn.instance.Case;

public interface CaseEvent extends ModelEvent {
    String TAG = "cafienne:case";

    default String getCaseInstanceId() {
        return this.getActorId();
    }
}
