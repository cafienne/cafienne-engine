/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemCollection;
import org.cafienne.cmmn.instance.casefile.InvalidPathException;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Event caused on the Case File
 */
public abstract class CaseFileEvent extends CaseBaseEvent {
    protected final static Logger logger = LoggerFactory.getLogger(CaseFileEvent.class);

    /**
     * The path to the case file item on which this event occurred (e.g., Order/Line[3])
     */
    public final Path path;

    protected CaseFileEvent(CaseFileItemCollection<?> item) {
        super(item.getCaseInstance());
        this.path = item.getPath();
    }

    protected CaseFileEvent(ValueMap json) {
        super(json);
        this.path = readPath(json, Fields.path);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    protected transient CaseFileItem caseFileItem;

    /**
     * Return the case file item's path through which the change was made (e.g., Order/Line)
     *
     * @return
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the index of the case file item within it's parent (or -1 if it is not an iterable case file item)
     *
     * @return
     */
    public int getIndex() {
        return path.getIndex();
    }

    @Override
    public void updateState(Case caseInstance) {
        try {
            // Resolve the path on the case file
            caseFileItem = path.resolve(caseInstance);
            updateState(caseFileItem);
        } catch (InvalidPathException shouldNotHappen) {
            logger.error("Could not recover path '" + path + "' on case instance?!", shouldNotHappen);
        }
    }

    abstract protected void updateState(CaseFileItem item);

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName() + "['" + path + "']";
    }

    public void writeCaseFileEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.path, path);
    }
}
