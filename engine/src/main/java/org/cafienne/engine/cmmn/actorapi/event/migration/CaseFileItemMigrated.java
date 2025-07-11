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

package org.cafienne.engine.cmmn.actorapi.event.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.actorapi.event.file.CaseFileEvent;
import org.cafienne.engine.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.engine.cmmn.instance.Path;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Event caused by migration of a CaseFileItem
 */
@Manifest
public class CaseFileItemMigrated extends CaseFileEvent {
    public final Path formerPath;

    public CaseFileItemMigrated(CaseFileItem item) {
        super(item);
        formerPath = item.getPreviousDefinition().getPath();
    }

    public CaseFileItemMigrated(ValueMap json) {
        super(json);
        this.formerPath = json.readPath(Fields.formerPath);
    }

    @Override
    protected void updateState(CaseFileItem item) {
        item.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseFileEvent(generator);
        writeField(generator, Fields.formerPath, formerPath);
    }
}
