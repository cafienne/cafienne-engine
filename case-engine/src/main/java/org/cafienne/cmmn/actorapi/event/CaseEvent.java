/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.BaseModelEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.actormodel.serialization.json.ValueMap;

import java.io.IOException;

public abstract class CaseEvent extends BaseModelEvent<Case> {
    public static final String TAG = "cafienne:case";

    protected CaseEvent(Case caseInstance) {
        super(caseInstance);
    }

    protected CaseEvent(ValueMap json) {
        super(json);
    }

    public final String getCaseInstanceId() {
        return this.getActorId();
    }

    protected void writeCaseInstanceEvent(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }

    @Override
    public void updateState(Case actor) {
    }
}
