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
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class CaseBaseEvent extends BaseModelEvent<Case> implements CaseEvent {
    protected CaseBaseEvent(Case caseInstance) {
        super(caseInstance);
    }

    protected CaseBaseEvent(ValueMap json) {
        super(json);
    }

    protected void writeCaseEvent(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }

    @Override
    public void updateState(Case actor) {
    }
}
