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

package org.cafienne.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.definition.casefile.PropertyDefinition;

import java.io.IOException;

public class BooleanValue extends PrimitiveValue<Boolean> {
    public BooleanValue(Boolean value) {
        super(value);
    }

    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        switch (propertyType) {
        case Boolean:
        case String: // Hmmm, do we really match strings?
        case Unspecified:
            return true;
        default:
            return baseMatch(propertyType);
        }
    }

    @Override
    public BooleanValue cloneValueNode() {
        return new BooleanValue(value);
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeBoolean(value);
    }
}