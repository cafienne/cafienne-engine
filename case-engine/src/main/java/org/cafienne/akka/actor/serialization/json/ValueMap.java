/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor.serialization.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.ValueMapJacksonDeserializer;
import org.cafienne.akka.actor.serialization.ValueMapJacksonSerializer;
import org.cafienne.cmmn.expression.spel.SpelReadable;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Name based {@link Map} of {@link Value} objects. Typically corresponds to a json object structure, and it is
 * aware of it's corresponding {@link CaseFileItem} and tries to map it's members to the case file item's structure.
 * Implemented with a {@link LinkedHashMap} in order to preserve the creation structure.
 */
@JsonSerialize(using = ValueMapJacksonSerializer.class)
@JsonDeserialize(using = ValueMapJacksonDeserializer.class)
public class ValueMap extends Value<Map<String, Value<?>>> implements SpelReadable {
    /**
     * Construct a new, empty value map
     */
    public ValueMap() {
        super(new LinkedHashMap());
    }

    /**
     * Creates a new ValueMap, while converting and adding the array of raw objects.
     * The array must have an even number of elements, in the shape:
     * <code>"fieldName1", value1, "fieldName2", value2, "fieldName3", value3, etc.</code>
     * The values are converted to Value objects, through the through the {@link Value#convert(Object)} method.
     * This also converts null objects into Value.NULL.
     *
     * @param rawInputs
     */
    public ValueMap(Object... rawInputs) {
        this();
        if (rawInputs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide sufficient input data to the ValueMap construction, of pattern String, Object, String, Object ...");
        }
        for (int i = 0; i < rawInputs.length; i += 2) {
            if (!(rawInputs[i] instanceof String || rawInputs[i] instanceof Fields)) {
                throw new IllegalArgumentException("Field name of parameter " + (i % 2) + " is not of type String or Fields, but it must be; found type " + rawInputs[i].getClass().getName());
            }
            putRaw(String.valueOf(rawInputs[i]), rawInputs[i + 1]);
        }
    }

    /**
     * Puts a field in the object. Returns the existing field with the same name if it was available.
     *
     * @param fieldName
     * @param fieldValue
     */
    public Value<?> put(String fieldName, Value<?> fieldValue) {
        return value.put(fieldName, fieldValue);
    }

    /**
     * Puts a field in the object. Returns the existing field with the same name if it was available.
     *
     * @param fieldName
     * @param fieldValue
     */
    public Value<?> put(Fields fieldName, Value<?> fieldValue) {
        return put(fieldName.toString(), fieldValue);
    }

    /**
     * Puts a raw value into the object, by first converting the raw value into a {@link Value} object.
     *
     * @param fieldName
     * @param rawValue
     */
    public void putRaw(String fieldName, Object rawValue) {
        put(fieldName, Value.convert(rawValue));
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        Iterator<Entry<String, Value<?>>> fields = value.entrySet().iterator();
        while (fields.hasNext()) {
            Entry<String, Value<?>> next = fields.next();
            printField(generator, next.getKey(), next.getValue());
        }
        generator.writeEndObject();
    }

    private void printField(JsonGenerator generator, String fieldName, Value<?> fieldValue) throws IOException {
        generator.writeFieldName(fieldName);
        fieldValue.print(generator);
    }

    @Override
    public boolean isSupersetOf(Value other) {
        if (other == null || !other.isMap()) {
            return false;
        }
        Map<String, Value<?>> thisMap = this.value;
        Map<String, Value<?>> otherMap = (Map<String, Value<?>>) other.value;
        // If the other map has more keys than we, we surely do not contain it.
        if (otherMap.size() > thisMap.size()) {
            return false;
        }
        // OtherMap has equal or less keys. Let's compare values for each key in the other map.
        for (String key : otherMap.keySet()) {
            Value thisValue = thisMap.get(key);
            Value otherValue = otherMap.get(key);
            if (thisValue == null || !thisValue.isSupersetOf(otherValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isMap() {
        return true;
    }

    @Override
    public void clearOwner() {
        super.clearOwner();
        value.values().forEach(v -> v.clearOwner());
    }

    /**
     * Returns a list of the field names in this object.
     *
     * @return
     */
    public Iterator<String> fieldNames() {
        return value.keySet().iterator();
    }

    /**
     * The get method always returns a non-null object.
     * If the field is not defined in the map, then a {@link Value#NULL} object is returned.
     *
     * @param fieldName
     * @return
     */
    public Value<?> get(String fieldName) {
        Value<?> fieldValue = value.get(fieldName);
        if (fieldValue == null) {
            fieldValue = Value.NULL;
        }
        return fieldValue;
    }

    public Value<?> get(Fields fieldName) {
        return get(fieldName.toString());
    }

    /**
     * Determines whether a field with the specified name is present in this ValueMap.
     *
     * @param fieldName
     * @return
     */
    public boolean has(String fieldName) {
        return value.containsKey(fieldName);
    }

    public boolean has(Fields fieldName) {
        return has(fieldName.toString());
    }

    /**
     * Returns the specified field if it exists and is a ValueMap.
     * If it does not exist, or the field is not a ValueMap, then it creates
     * a ValueMap object and puts it inside this ValueMap (i.e., it overrides an existing Value<?> if that is not a ValueMap).
     *
     * @param fieldName
     * @return
     */
    public ValueMap with(String fieldName) {
        Value<?> v = value.get(fieldName);
        if (!(v instanceof ValueMap)) {
            v = new ValueMap();
            value.put(fieldName, v);
        }
        return v.asMap();
    }

    public ValueMap with(Fields fieldName) {
        return with(fieldName.toString());
    }

    /**
     * Similar to {@link #with(String)} method, except that it now works for array objects: if a field with the specified name is
     * not found or it is not an array, then it will be replaced with an empty array.
     *
     * @param fieldName
     * @return
     */
    public ValueList withArray(String fieldName) {
        Value<?> v = value.get(fieldName);
        if (!(v instanceof ValueList)) {
            v = new ValueList();
            value.put(fieldName, v);
        }
        return v.asList();
    }

    public ValueList withArray(Fields fieldName) {
        return withArray(fieldName.toString());
    }

    /**
     * Returns a direct cast of the raw value of the field to the expected return type,
     * returns null if the field is not present.
     *
     * @param fieldName
     * @return
     */
    public <T> T raw(String fieldName) {
        @SuppressWarnings("unchecked")
        T rawValue = (T) get(fieldName).value;
        return rawValue;
    }

    public <T> T raw(Fields fieldName) {
        return this.raw(fieldName.toString());
    }

    /**
     * Casts a LongValue to int.
     *
     * @param fieldName
     * @return
     */
    public int rawInt(String fieldName) {
        Value<?> v = get(fieldName);
        return ((Long) v.value).intValue();
    }

    public int rawInt(Fields fieldName) {
        return rawInt(fieldName.toString());
    }

    /**
     * Returns the field with specified name as an Instant
     *
     * @param fieldName
     * @return
     */
    public Instant rawInstant(String fieldName) {
        Value<?> v = get(fieldName);
        if (v == Value.NULL) {
            return null;
        }
        return Instant.parse(v.value.toString());
    }

    public Instant rawInstant(Fields fieldName) {
        return this.rawInstant(fieldName.toString());
    }

    public <T extends Enum<?>> T getEnum(Fields fieldName, Class<T> tClass) {
        return getEnum(fieldName.toString(), tClass);
    }

    /**
     * Hackerish method to parse a String back into an Enum
     *
     * @param fieldName
     * @param tClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<?>> T getEnum(String fieldName, Class<T> tClass) {
        Value<?> v = get(fieldName);
        if (v == null || v == Value.NULL) {
            return null;
        }
        String string = raw(fieldName);
        if (string == null) {
            // No value found, just return null;
            return null;
        }
        // Try to find valueOf method and use that to instantiate the enum.
        try {
            Method m = tClass.getMethod("valueOf", String.class);
            return (T) m.invoke(tClass, string);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(e.fillInStackTrace());
        }
    }

    @Override
    public boolean canRead(String propertyName) {
        // Initially this code was delegating to owner if available, but spec says we should always return "something"
        // See also {@link CaseFileItem#canRead(String)}
        return true;
    }

    @Override
    public Value<?> read(String propertyName) {
        // If there is an owner, we check whether the property is one of it's CaseFileItem children.
        if (getOwner() != null) {
            // Bit of tricky code here.
            // Basic idea: property is either a child case file item, or one of the properties of the case file item, _OR_ it is in the raw content of the
            // case file item.
            // Properties are stored in the raw content as well, so we only have to check whether there is a child item with the specified property name,
            // if so, we search further in the child. Else we default to go lookup the property in our raw value structure
            CaseFileItem child = getOwner().getItem(propertyName);
            if (child != null) {
                return child.getValue();
            }
        }

        // Otherwise we read through our getter. If it doesn't contain the property, then it returns a null wrapper, rather than null, in order
        // not to 'crash' any expressions; CMMN spec says we should return empty rather than null elements.
        //  NOTE: in some cases still the expression may crash, e.g. if the NullValue is compared to an int or so...
        return get(propertyName);
    }

    @Override
    public ValueMap cloneValueNode() {
        ValueMap clone = new ValueMap();
        Iterator<Entry<String, Value<?>>> fields = value.entrySet().iterator();
        while (fields.hasNext()) {
            Entry<String, Value<?>> childItem = fields.next();
            clone.value.put(childItem.getKey(), childItem.getValue().cloneValueNode());
        }
        return clone;
    }

    @Override
    public <T extends Value> T merge(T withValue) {
        if (!(withValue.isMap())) {
            return withValue;
        }
        withValue.asMap().value.forEach((fieldName, fromFieldValue) -> {
            Value<?> myFieldValue = this.value.get(fieldName);
            // If we have a value, we ought to merge the other value into it.
            if (myFieldValue != null) {
                fromFieldValue = myFieldValue.merge(fromFieldValue);
            }
            // And now overwrite our value
            this.value.put(fieldName, fromFieldValue);
        });
        return (T) this;
    }
}
