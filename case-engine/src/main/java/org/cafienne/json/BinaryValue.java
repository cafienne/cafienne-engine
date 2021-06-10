/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;

import java.io.IOException;

public class BinaryValue extends PrimitiveValue<byte[]> {
    BinaryValue(byte[] bytes) {
        super(bytes);
    }

    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        switch (propertyType) {
        case Base64Binary:
        case String: // Hmmm, do we really match strings?
        case Unspecified:
            return true;
        default:
            return baseMatch(propertyType);
        }
    }

    @Override
    public BinaryValue cloneValueNode() {
        return new BinaryValue(value); // Or ... should we also copy/clone the bytearry?
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeBinary(value);
    }
}