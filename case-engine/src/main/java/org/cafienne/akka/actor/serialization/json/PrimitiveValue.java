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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;

public abstract class PrimitiveValue<T> extends Value<T> {
    public PrimitiveValue(T value) {
        super(value);
    }

    @Override
    public final boolean isPrimitive() {
        return true;
    }

    @Override
    public String toString() {
        // Avoid that toString() goes via the print implementation, because that causes StringValues to be wrapped with quotes
        return String.valueOf(getValue());
    }

    @Override
    public abstract boolean matches(PropertyDefinition.PropertyType propertyType);

    @Override
    public abstract void print(JsonGenerator generator) throws IOException;

    @Override
    public boolean isSupersetOf(Value otherValue) {
        return otherValue != null && this.value.equals(otherValue.value);
    }

    @Override
    public Value<?> merge(Value<?> withValue) {
        // Primitives cannot merge, so we always return the other value; it simply overwrites our value.
        return withValue;
    }

    @Override
    public void dumpMemoryStateToXML(Element parentElement) {
        Node valueNode = parentElement.getOwnerDocument().createTextNode(String.valueOf(value));
        parentElement.appendChild(valueNode);
    }
}
