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
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.infrastructure.serialization.CafienneSerializable;

import java.io.IOException;

public class CafienneSerializableValue extends PrimitiveValue<CafienneSerializable> {
    public CafienneSerializableValue(CafienneSerializable value) {
        super(value);
    }

    @Override
    public CafienneSerializableValue cloneValueNode() {
        return new CafienneSerializableValue(value);
    }

    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        return propertyType == PropertyDefinition.PropertyType.Unspecified;
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        value.writeThisObject(generator);
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }
}