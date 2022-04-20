/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.expression.spel.SpelReadable;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.ValueMapJacksonDeserializer;
import org.cafienne.infrastructure.serialization.ValueMapJacksonSerializer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
        super(new LinkedHashMap<>());
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
        plus(rawInputs);
    }

    /**
     * Add a number of properties to the map.
     * Note, the properties must come as name/value pairs.
     *
     * @param rawInputs
     * @return
     */
    public ValueMap plus(Object... rawInputs) {
        if (rawInputs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide sufficient input data to the ValueMap construction, of pattern String, Object, String, Object ...");
        }
        for (int i = 0; i < rawInputs.length; i += 2) {
            if (rawInputs[i] == null) {
                throw new IllegalArgumentException("Field name cannot be null (argument nr " + i + ")");
            }
            String fieldName = String.valueOf(rawInputs[i]);
            if ((fieldName.length() > 50 && !(rawInputs[i] instanceof String || rawInputs[i] instanceof Fields))) {
                throw new IllegalArgumentException("Field name at argument nr " + i + " is type " + rawInputs[i].getClass().getName() + " generates a field name with too many characters (" + fieldName.length() + "). Probably wrong argument order? Otherwise use put() method instead for this field.");
            }
            put(fieldName, Value.convert(rawInputs[i + 1]));
        }
        return this;
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

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        printFields(generator);
        generator.writeEndObject();
    }

    public void printFields(JsonGenerator generator) throws IOException {
        for (Entry<String, Value<?>> next : value.entrySet()) {
            printField(generator, next.getKey(), next.getValue());
        }
    }

    private void printField(JsonGenerator generator, String fieldName, Value<?> fieldValue) throws IOException {
        generator.writeFieldName(fieldName);
        fieldValue.print(generator);
    }

    @Override
    public boolean isSupersetOf(Value<?> other) {
        if (other == null || !other.isMap()) {
            return false;
        }
        Map<String, Value<?>> thisMap = this.value;
        Map<String, Value<?>> otherMap = other.asMap().value;
        // If the other map has more keys than we, we surely do not contain it.
        if (otherMap.size() > thisMap.size()) {
            return false;
        }
        // OtherMap has equal or less keys. Let's compare values for each key in the other map.
        for (String key : otherMap.keySet()) {
            Value<?> thisValue = thisMap.get(key);
            Value<?> otherValue = otherMap.get(key);
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

    private <T extends Enum<?>> T getEnum(Fields fieldName, Class<T> tClass) {
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
    private <T extends Enum<?>> T getEnum(String fieldName, Class<T> tClass) {
        Value<?> v = get(fieldName);
        if (v == null || v == Value.NULL) {
            return null;
        }

        String string = String.valueOf(v.value);
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
        // Simply "get". If it is a case file item child, then it's value will also be in the parent's valuemap with the same name.
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
    public <V extends Value<?>> V merge(V withValue) {
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
        return (V) this;
    }

    @SafeVarargs
    public final <T> T readField(Fields fieldName, T... value) {
        if (has(fieldName)) {
            return raw(fieldName);
        } else if (value.length > 0) {
            return value[0];
        } else {
            return null;
        }
    }

    public <T extends Enum<?>> T readEnum(Fields fieldName, Class<T> enumClass) {
        return getEnum(fieldName, enumClass);
    }

    public String readString(Fields fieldName, String... value) {
        return readField(fieldName, value);
    }

    public Boolean readBoolean(Fields fieldName, Boolean... value) {
        if (value.length > 0) {
            return readField(fieldName, value);
        } else {
            return readField(fieldName, false);
        }
    }

    public Instant readInstant(Fields fieldName) {
        return rawInstant(fieldName);
    }

    public String[] readStringList(Fields fieldName) {
        List<String> list = withArray(fieldName).rawList();
        return list.toArray(new String[0]);
    }

    public ValueMap readMap(Fields fieldName) {
        return with(fieldName);
    }

    public <T> Set<T> readSet(Fields fieldName) {
        return new HashSet<>(withArray(fieldName).rawList());
    }

    public <T extends CMMNElementDefinition> T readDefinition(Fields fieldName, Class<T> tClass) {
        return CMMNElementDefinition.fromJSON(this.getClass().getName(), readMap(fieldName), tClass);
    }

    public Path readPath(Fields fieldName) {
        return new Path(readString(fieldName));
    }

    public <T> T readObject(Fields fieldName, ValueMapParser<T> parser) {
        ValueMap json = with(fieldName);
        return parser.convert(json);
    }

    public <T> List<T> readObjects(Fields fieldName, ValueMapParser<T> parser) {
        return withArray(fieldName).stream().map(json -> parser.convert(json.asMap())).collect(Collectors.toList());
    }
}
