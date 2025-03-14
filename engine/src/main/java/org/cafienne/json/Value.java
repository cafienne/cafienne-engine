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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class Value<T> implements Serializable {
    /**
     * Wrapper around <code>null</code>.
     */
    public static final Value<?> NULL = new NullValue();

    /**
     * Contains the raw, typed value. This member is protected so that
     * Value classes can directly access it.
     */
    protected final T value;

    private transient CaseFileItem owner;

    protected Value(T value) {
        this.value = value;
    }

    /**
     * Returns the case file item to which this value belongs. Can be null, e.g. in the case of parameter passing.
     *
     * @return
     */
    public CaseFileItem getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this value, i.e., the case file item to which it belongs.
     *
     * @param caseFileItem
     */
    public void setOwner(CaseFileItem caseFileItem) {
        this.owner = caseFileItem;
    }

    public void clearOwner() {
        this.setOwner(null);
    }

    /**
     * Returns the raw, typed value
     *
     * @return
     */
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        JsonFactory factory = new JsonFactory();
        StringWriter sw = new StringWriter();
        try (JsonGenerator generator = factory.createGenerator(sw)) {
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            print(generator);
        } catch (IOException e) {
            // Hmmm... scary... not sure if this is a proper solution
            sw.write("Could not print the value because of error");
            e.printStackTrace(new PrintWriter(sw));
        }
        return sw.toString();
    }

    /**
     * Returns true if "this" value equals or contains the "other" value.
     * Note, values must have same type. E.g., a ValueList.contains(someValueMap) returns false, even
     * if the value list has an element that equals the ValueMap. It would return for
     * ValueList.contains(new ValueList(someValueMap)).
     * For PrimitiveValue this is a plain comparison on the internal, typed value.
     *
     * @param otherValue Value that should be contained within this value
     * @return
     */
    public boolean isSupersetOf(Value<?> otherValue) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return this.value == null || this == Value.NULL; // Hmmm, we equals null??
        } else if (obj instanceof Value) { // Hmmm, check the others value
            Value<?> that = (Value<?>) obj;
            if (this.value == null) {
                return that.value == null;
            }
            return this.value.equals(that.value);
        } else { // Hmmm, maybe a bit tricky - comparing our value against the object
            if (value == null) {
                return false;
            }
            return value.equals(obj);
        }
    }

    /**
     * Indicates whether the value is primitive or not (in which case it is probably an object or array type of value)
     *
     * @return
     */
    public abstract boolean isPrimitive();

    public boolean isList() {
        return false;
    }

    public ValueList asList() {
        return (ValueList) this;
    }

    public boolean isMap() {
        return false;
    }

    public ValueMap asMap() {
        return (ValueMap) this;
    }

    public void print(JsonGenerator generator) throws IOException {
    }

    /**
     * Indicates whether the raw value matches the property type definition
     *
     * @param propertyType
     * @return
     */
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        return propertyType == PropertyDefinition.PropertyType.Unspecified; // By default we can only handle unspecified types
    }

    protected boolean baseMatch(PropertyDefinition.PropertyType propertyType) {
        return propertyType == PropertyDefinition.PropertyType.Unspecified; // By default we can only handle unspecified types
    }

    /**
     * Merge the other value into this value. Only interesting for container nodes. For primitive nodes
     * it will return the withValue.
     *
     * @param withValue - Value to be merged into this
     */
    public abstract <V extends Value<?>> V merge(V withValue);

    /**
     * Converts a (possibly not-Serializable) value to something that can be serialized.
     *
     * @param object Raw object that is inspected for conversion.
     * @return
     */
    public static Value<?> convert(Object object) {
        // NOTE: order of if statements is measured by running Cafienne TypeScript test framework;
        //  String convert happens 6500 times, Seq conversion 400, Array conversion 200, etc.
        if (object == null) {
            return Value.NULL;
        } else if (object instanceof Value<?>) {
            return (Value<?>) object;
        } else if (object instanceof String) {
            return new StringValue((String) object);
        } else if (object instanceof scala.collection.Map) {
            scala.collection.Map<?, ?> map = (scala.collection.Map<?, ?>) object;
            ValueMap valueMap = new ValueMap();
            map.foreach(entry -> valueMap.put(String.valueOf(entry._1()), convert(entry._2())));
            return valueMap;
        } else if (object instanceof scala.Option) {
            scala.Option<?> option = (scala.Option<?>) object;
            if (option.isEmpty()) {
                return Value.NULL;
            } else {
                return Value.convert(option.get());
            }
        } else if (object instanceof scala.collection.Iterable) {
            scala.collection.Iterable<?> list = (scala.collection.Iterable<?>) object;
            ValueList valueList = new ValueList();
            list.foreach(item -> valueList.add(convert(item)));
            return valueList;
        } else if (object instanceof Object[] || object.getClass().isArray()) {
            // We first check with instanceof, and then with isArray, because isArray is slower, and instanceof does not recognize primitive types.
            ValueList valueList = new ValueList();
            for (int i = 0; i < Array.getLength(object); i++) {
                Object o = Array.get(object, i);
                valueList.add(convert(Array.get(object, i)));
            }
            return valueList;
        } else if (object instanceof CafienneJson) {
            return ((CafienneJson) object).toValue();
        } else if (object instanceof Collection) {
            Collection<?> list = (Collection<?>) object;
            ValueList valueList = new ValueList();
            for (Object element : list) {
                valueList.add(convert(element));
            }
            return valueList;
        } else if (object instanceof Boolean) {
            return new BooleanValue((Boolean) object);
        } else if (object instanceof Double || object instanceof Float) {
            return new DoubleValue((Double) object);
        } else if (object instanceof Long) {
            return new LongValue((Long) object);
        } else if (object instanceof Integer) {
            return new LongValue((Integer) object);
        } else if (object instanceof Short) {
            return new LongValue((Short) object);
        } else if (object instanceof Instant) {
            return new InstantValue((Instant) object);
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            ValueMap valueMap = new ValueMap();
            for (Entry<?, ?> entry : map.entrySet()) {
                valueMap.put(String.valueOf(entry.getKey()), convert(entry.getValue()));
            }
            return valueMap;
        } else if (object instanceof Set) {
            Set<?> list = (Set<?>) object;
            ValueList valueList = new ValueList();
            for (Object element : list) {
                valueList.add(convert(element));
            }
            return valueList;
        } else if (object instanceof Throwable) {
            return convertThrowable((Throwable) object);
        } else if (object instanceof CafienneSerializable) {
            return new CafienneSerializableValue((CafienneSerializable) object);
        } else if (object instanceof Serializable) {
            return new SerializableValue(object);
        } else {
            return new NonSerializableValue(object);
        }
    }

    /**
     * Recursive function that also contains exception cause
     *
     * @param throwable
     * @return
     */
    public static ValueMap convertThrowable(Throwable throwable) {
        ValueMap value = new ValueMap();
        value.put("className", new StringValue(throwable.getClass().getName()));
        value.put("message", new StringValue(throwable.getLocalizedMessage()));
        ValueList valueList = value.withArray("lines");
        StackTraceElement[] trace = throwable.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement traceElement = trace[i];
            valueList.add(new StringValue(traceElement.toString()));
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            value.put("cause", convertThrowable(cause));
        }
        return value;
    }

    /**
     * Creates a clone of this value. Note: this clone is always a "deep" clone.
     *
     * @return
     */
    public abstract Value<?> cloneValueNode();

    public void dumpMemoryStateToXML(Element parentElement) {
        String myStringValue = toString();
        Node valueNode = parentElement.getOwnerDocument().createTextNode(myStringValue);
        parentElement.appendChild(valueNode);
    }
}
