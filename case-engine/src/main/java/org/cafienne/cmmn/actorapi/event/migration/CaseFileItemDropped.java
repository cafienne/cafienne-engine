/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.file.CaseFileEvent;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Event caused by migration of a CaseFileItem
 */
@Manifest
public class CaseFileItemDropped extends CaseFileEvent {
    public CaseFileItemDropped(CaseFileItem item) {
        super(item);
    }

    public CaseFileItemDropped(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateState(CaseFileItem item) {
        item.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseFileEvent(generator);
    }
}
