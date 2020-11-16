/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.instance.*;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.casefile.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Event caused on a CaseFileItem
 */
@Manifest
public abstract class CaseFileBaseEvent extends CaseEvent {
    protected final static Logger logger = LoggerFactory.getLogger(CaseFileBaseEvent.class);

    /**
     * The path to the case file item on which this event occurred (e.g., Order/Line[3])
     */
    public final Path path;

    public CaseFileBaseEvent(CaseFileItemCollection<?> item) {
        super(item.getCaseInstance());
        this.path = item.getPath();
    }

    public CaseFileBaseEvent(ValueMap json) {
        super(json);
        this.path = readPath(json, Fields.path);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    protected transient CaseFileItem caseFileItem;

    @Override
    public void updateState(Case caseInstance) {
        try {
            // Resolve the path on the case file
            caseFileItem = path.resolve(caseInstance);
            updateState(caseFileItem);
        } catch (InvalidPathException shouldNotHappen) {
            logger.error("Could not recover path on case instance?!", shouldNotHappen);
        }
    }

    abstract protected void updateState(CaseFileItem item);

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName() + "['" + path + "']";
    }

    public void writeCaseFileEvent(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.path, path);
    }
}
