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

package org.cafienne.engine.cmmn.actorapi.event;

import java.io.IOException;

import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import com.fasterxml.jackson.core.JsonGenerator;

@Manifest
public class CaseOutputFilled extends CaseBaseEvent {
    public final ValueMap output;
    public CaseOutputFilled(Case actor, ValueMap outputParameters) {
        super(actor);
        this.output = outputParameters;
    }

    public CaseOutputFilled(ValueMap json) {
        super(json);
        this.output = json.readMap(Fields.output);
    }

    @Override
    public void updateState(Case actor) {
        // No need to update state, as that is also updated through the case file events
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.output, output);
    }
}
