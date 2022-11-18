/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemCollection;
import org.cafienne.cmmn.instance.casefile.InvalidPathException;
import org.cafienne.cmmn.instance.Path;
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
        this.path = json.readPath(Fields.path);
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
