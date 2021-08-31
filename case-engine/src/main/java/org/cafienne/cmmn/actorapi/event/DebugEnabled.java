/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * DebugEvent
 */
@Manifest
public class DebugEnabled extends CaseEvent {
    public DebugEnabled(Case caseInstance) {
        super(caseInstance);
    }

    public DebugEnabled(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(Case actor) {
        actor.setDebugMode(true);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
    }
}
