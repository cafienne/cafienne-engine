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

public class NonSerializableValue extends PrimitiveValue<Object> {
    private final Object value;

    public NonSerializableValue(Object value) {
        super(String.valueOf(value));
        this.value = value;
    }

    @Override
    public NonSerializableValue cloneValueNode() {
        return new NonSerializableValue(value);
    }

    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        return propertyType == PropertyDefinition.PropertyType.Unspecified;
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeObject(value);
    }

    @Override
    public Object getValue() {
        return value;
    }
}