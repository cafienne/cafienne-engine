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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.expression.spel.SpelReadable;
import org.cafienne.engine.cmmn.instance.Path;
import org.cafienne.engine.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.infrastructure.serialization.*;
import org.cafienne.infrastructure.serialization.serializers.CommandSerializers;

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

    public static ValueMap fill(ValueMapFiller filler) {
        ValueMap map = new ValueMap();
        filler.fill(map);
        return map;
    }

    @FunctionalInterface
    public interface ValueMapFiller {
        void fill(ValueMap map);
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
    public Value<?> put(Object fieldName, Value<?> fieldValue) {
        return value.put(String.valueOf(fieldName), fieldValue);
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
        value.values().forEach(Value::clearOwner);
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
    public Value<?> get(Object fieldName) {
        Value<?> fieldValue = value.get(String.valueOf(fieldName));
        if (fieldValue == null) {
            fieldValue = Value.NULL;
        }
        return fieldValue;
    }

    /**
     * Determines whether a field with the specified name is present in this ValueMap.
     *
     * @param fieldName
     * @return
     */
    public boolean has(Object fieldName) {
        return value.containsKey(String.valueOf(fieldName));
    }

    /**
     * Returns the specified field if it exists and is a ValueMap.
     * If it does not exist, or the field is not a ValueMap, then it creates
     * a ValueMap object and puts it inside this ValueMap (i.e., it overrides an existing Value<?> if that is not a ValueMap).
     *
     * @param fieldName
     * @return
     */
    public ValueMap with(Object fieldName) {
        String field = String.valueOf(fieldName);
        Value<?> v = value.get(field);
        if (!(v instanceof ValueMap)) {
            v = new ValueMap();
            value.put(field, v);
        }
        return v.asMap();
    }

    /**
     * Similar to {@link #with(Object)} method, except that it now works for array objects: if a field with the specified name is
     * not found or it is not an array, then it will be replaced with an empty array.
     *
     * @param fieldName
     * @return
     */
    public ValueList withArray(Object fieldName) {
        String field = String.valueOf(fieldName);
        Value<?> v = value.get(field);
        if (!(v instanceof ValueList)) {
            v = new ValueList();
            value.put(field, v);
        }
        return v.asList();
    }

    /**
     * Returns a direct cast of the raw value of the field to the expected return type,
     * returns null if the field is not present.
     *
     * @param fieldName
     * @return
     */
    public <T> T raw(Object fieldName) {
        @SuppressWarnings("unchecked")
        T rawValue = (T) get(String.valueOf(fieldName)).value;
        return rawValue;
    }

    /**
     * Casts a LongValue to int.
     *
     * @param fieldName
     * @return
     */
    public int rawInt(Object fieldName) {
        Value<?> v = get(String.valueOf(fieldName));
        return ((Long) v.value).intValue();
    }

    /**
     * Returns the field with specified name as an Instant
     *
     * @param fieldName
     * @return
     */
    public Instant rawInstant(Object fieldName, Instant... value) {
        Value<?> v = get(String.valueOf(fieldName));
        if (v == Value.NULL) {
            return value.length > 0 ? value[0] : null;
        }
        return Instant.parse(v.value.toString());
    }

    /**
     * Hackerish method to parse a String back into an Enum
     *
     * @param fieldName
     * @param tClass
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<?>> T getEnum(Object fieldName, Class<T> tClass) {
        Value<?> v = get(String.valueOf(fieldName));
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
    public final <T> T readField(Object fieldName, T... defaultValue) {
        if (has(fieldName)) {
            return raw(fieldName);
        } else if (defaultValue.length > 0) {
            return defaultValue[0];
        } else {
            return null;
        }
    }

    public <T extends Enum<?>> T readEnum(Object fieldName, Class<T> enumClass) {
        return getEnum(fieldName, enumClass);
    }

    public String readString(Object fieldName, String... value) {
        return readField(fieldName, value);
    }

    public Boolean readBoolean(Object fieldName, Boolean... value) {
        if (value.length > 0) {
            return readField(fieldName, value);
        } else {
            return readField(fieldName, false);
        }
    }

    public Long readLong(Object fieldName, Long... value) {
        if (value.length > 0) {
            return readField(fieldName, value);
        } else {
            return readField(fieldName);
        }
    }

    public Instant readInstant(Object fieldName, Instant... value) {
        return rawInstant(fieldName, value);
    }

    public String[] readStringList(Object fieldName) {
        String[] emptyArray = new String[0];
        if (has(fieldName)) { // Avoid creating array in the json if it is not defined.
            List<String> list = withArray(fieldName).rawList();
            return list.toArray(emptyArray);
        } else {
            return emptyArray;
        }
    }

    public ValueMap readMap(Object fieldName) {
        return with(fieldName);
    }

    public <T> Set<T> readSet(Object fieldName) {
        if (has(fieldName)) { // Avoid creating array in the json if it is not defined.
            return new HashSet<>(withArray(fieldName).rawList());
        } else {
            return new HashSet<>();
        }
    }

    public <T extends CMMNElementDefinition> T readDefinition(Object fieldName, Class<T> tClass) {
        return CMMNElementDefinition.fromJSON(this.getClass().getName(), readMap(fieldName), tClass);
    }

    public <T extends CafienneSerializable> T readManifestField(Object fieldName) {
        return CafienneSerializer.deserialize(readMap(fieldName));
    }

    public Path readPath(Object fieldName, String ...value) {
        return new Path(readString(fieldName, value));
    }

    public <T> T readObject(Object fieldName, ValueMapParser<T> parser) {
        if (has(fieldName) && get(fieldName) != Value.NULL) {
            ValueMap json = with(fieldName);
            return parser.convert(json);
        } else {
            return null;
        }
    }

    public <T> List<T> readObjects(Object fieldName, ValueMapParser<T> parser) {
        if (has(fieldName)) { // Avoid creating array in the json if it is not defined.
            return withArray(fieldName).stream().map(json -> parser.convert(json.asMap())).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public final scala.Option<ValueMap> readOptionalMap(Object fieldName) {
        Value<?> value = get(fieldName);
        return value == Value.NULL ? scala.Option.apply(null) : new scala.Some<>(value.asMap());
    }

    public final <T> scala.Option<T> readOption(Object fieldName) {
        T rawValue = raw(fieldName);
        return rawValue == null ? scala.Option.apply(null) : new scala.Some<>(rawValue);
    }
}

