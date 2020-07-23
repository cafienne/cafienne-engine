/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor.serialization.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;

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