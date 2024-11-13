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

package com.casefabric.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.definition.casefile.PropertyDefinition;
import com.casefabric.cmmn.expression.spel.SpelPropertyValueProvider;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;

public abstract class PrimitiveValue<T> extends Value<T> implements SpelPropertyValueProvider {
    public PrimitiveValue(T value) {
        super(value);
    }

    @Override
    public boolean isPrimitive() {
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
    public boolean isSupersetOf(Value<?> otherValue) {
        return otherValue != null && this.value.equals(otherValue.value);
    }

    @Override
    public <V extends Value<?>> V merge(V withValue) {
        // Primitives cannot merge, so we always return the other value; it simply overwrites our value.
        return withValue;
    }

    @Override
    public void dumpMemoryStateToXML(Element parentElement) {
        Node valueNode = parentElement.getOwnerDocument().createTextNode(String.valueOf(value));
        parentElement.appendChild(valueNode);
    }
}
