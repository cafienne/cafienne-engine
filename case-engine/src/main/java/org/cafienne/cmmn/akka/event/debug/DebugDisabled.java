/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event.debug;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseInstanceEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

/**
 * DebugEvent
 */
@Manifest
public class DebugDisabled extends CaseInstanceEvent {
    public DebugDisabled(Case caseInstance) {
        super(caseInstance);
    }

    public DebugDisabled(ValueMap json) {
        super(json);
    }

    @Override
    public void recover(Case caseInstance) {
        caseInstance.switchDebugMode(false);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
    }
}
