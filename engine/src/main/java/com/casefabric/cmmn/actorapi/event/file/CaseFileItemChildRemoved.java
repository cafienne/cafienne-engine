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

package com.casefabric.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.casefile.CaseFileItemCollection;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import com.casefabric.cmmn.instance.casefile.InvalidPathException;
import com.casefabric.cmmn.instance.Path;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * Event caused by replacement of a CaseFileItem for children of that item that have to be removed
 */
@Manifest
public class CaseFileItemChildRemoved extends CaseFileItemTransitioned {
    private final Path childPath;

    public CaseFileItemChildRemoved(CaseFileItemCollection<?> item, Path childPath) {
        super(item, State.Available, CaseFileItemTransition.RemoveChild, Value.NULL);
        this.childPath = childPath;
    }

    public CaseFileItemChildRemoved(ValueMap json) {
        super(json);
        this.childPath = json.readPath(Fields.childPath);
    }

    public Path getChildPath() {
        return childPath;
    }

    @Override
    public boolean hasBehavior() {
        return false;
    }

    @Override
    public void updateState(Case caseInstance) {
        try {
            // Resolve the path on the case file.
            //  Note: we need to override this method instead of implementing the updateState(CaseFileItem item) method,
            //  since we need to find the host that dropped this child, and host can be the CaseFile as well.
            CaseFileItemCollection<?> host = path.resolve(caseInstance);
            host.updateState(this);
        } catch (InvalidPathException shouldNotHappen) {
            logger.error("Could not recover path on case instance?!", shouldNotHappen);
        }
    }

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName() + "['" + path.getPart() + "']. child: [" + childPath + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.childPath, childPath);
    }
}
