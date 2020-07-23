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

public class SerializableValue extends PrimitiveValue<Object> {
    public SerializableValue(Object value) {
        super(value);
    }

    @Override
    public SerializableValue cloneValueNode() {
        return new SerializableValue(value);
    }

    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        return propertyType == PropertyDefinition.PropertyType.Unspecified;
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeObject(String.valueOf(value));
    }
}