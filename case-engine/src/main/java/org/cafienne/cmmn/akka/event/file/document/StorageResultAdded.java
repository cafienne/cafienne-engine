/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event.file.document;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.event.file.CaseFileBaseEvent;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.document.StorageResult;

import java.io.IOException;
import java.util.List;

/**
 * Event caused by creation of a CaseFileItem
 */
@Manifest
public class StorageResultAdded extends CaseFileBaseEvent {
    public final List<StorageResult> storageResult;

    public StorageResultAdded(CaseFileItem item, List<StorageResult> storageResult) {
        super(item);
        this.storageResult = storageResult;
    }

    public StorageResultAdded(ValueMap json) {
        super(json);
        this.storageResult = readList(json, Fields.storageResult, v -> StorageResult.deserialize(v));
    }

    @Override
    public void updateState(CaseFileItem item) {
        item.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseFileEvent(generator);
        writeListField(generator, Fields.storageResult, storageResult);
    }
}
