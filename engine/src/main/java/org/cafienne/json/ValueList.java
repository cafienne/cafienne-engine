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

import java.io.IOException;
import java.util.*;

/**
 * {@link List} based set of {@link Value} objects. Typically corresponds to a json array structure.
 */
public class ValueList extends Value<List<Value<?>>> implements List<Value<?>> {
    /**
     * Creates a new ValueList, while converting and adding the array of raw objects
     * passed as input (through the {@link Value#convert(Object)} method.
     *
     * @param rawItems
     */
    public ValueList(Object... rawItems) {
        super(new ArrayList<>());
        for (Object listItem : rawItems) {
            add(convert(listItem));
        }
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public void clearOwner() {
        super.clearOwner();
        getValue().forEach(Value::clearOwner);
    }

    @Override
    public boolean isSupersetOf(Value<?> otherValue) {
        if (otherValue == null || !otherValue.isList()) {
            return false;
        }
        ValueList otherList = otherValue.asList();
        // Compare sizes
        if (otherList.size() > this.size()) {
            return false;
        }
        // Compare contents
        for (int i = 0; i < otherList.size(); i++) {
            Value<?> thisListItem = this.get(i);
            Value<?> otherListItem = otherList.get(i);
            if (!thisListItem.isSupersetOf(otherListItem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeStartArray(value, value.size());

        for (Value<?> item : value) {
            if (item != null) {
                item.print(generator);
            } else {
                generator.writeNull();
            }
        }

        generator.writeEndArray();
    }

    @Override
    public Iterator<Value<?>> iterator() {
        return getValue().iterator();
    }

    @Override
    public boolean add(Value<?> e) {
        return getValue().add(e);
    }

    @Override
    public void add(int index, Value<?> element) {
        getValue().add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends Value<?>> c) {
        return getValue().addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Value<?>> c) {
        return getValue().addAll(index, c);
    }

    @Override
    public void clear() {
        getValue().clear();
    }

    @Override
    public boolean contains(Object o) {
        return getValue().contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getValue().containsAll(c);
    }

    @Override
    public Value<?> get(int index) {
        if (index < 0 || index > value.size() - 1) {
            return NULL;
        }
        return getValue().get(index);
    }

    @Override
    public int indexOf(Object o) {
        return getValue().indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    @Override
    public int lastIndexOf(Object o) {
        return getValue().lastIndexOf(o);
    }

    @Override
    public ListIterator<Value<?>> listIterator() {
        return getValue().listIterator();
    }

    @Override
    public ListIterator<Value<?>> listIterator(int index) {
        return getValue().listIterator(index);
    }

    @Override
    public boolean remove(Object o) {
        return getValue().remove(o);
    }

    @Override
    public Value<?> remove(int index) {
        return getValue().remove(index);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return getValue().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return getValue().retainAll(c);
    }

    @Override
    public Value<?> set(int index, Value<?> element) {
        return getValue().set(index, element);
    }


    @Override
    public int size() {
        return getValue().size();
    }

    @Override
    public List<Value<?>> subList(int fromIndex, int toIndex) {
        return getValue().subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return getValue().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return getValue().toArray(a);
    }

    /**
     * Convert raw values of the list to typed array
     *
     * @param <T>
     * @return
     */
    public <T> List<T> rawList() {
        List<Value<?>> values = getValue();
        List<T> list = new ArrayList<>();
        values.forEach(value -> list.add((T) value.getValue()));
        return list;
    }

    @Override
    public ValueList cloneValueNode() {
        ValueList clone = new ValueList();
        int num = value.size();
        for (int i = 0; i < num; i++) {
            Value<?> element = value.get(i);
            clone.add(element.cloneValueNode());
        }

        return clone;
    }

    @Override
    public <V extends Value<?>> V merge(V withValue) {
        if (! (withValue.isList())) {
            return withValue;
        }
        ValueList fromList = withValue.asList();
        for (int i = 0; i < fromList.size(); i++) {
            Value<?> fromValue = fromList.get(i);
            if (i < this.size()) {
                // Merge into existing value (and replace, since merge may or may not return a new object reference)
                this.set(i, this.get(i).merge(fromValue));
            } else {
                // Simply extend this array
                this.add(fromValue);
            }
        }
        return (V) this;
    }
}
