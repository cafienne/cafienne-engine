/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor.serialization.json;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.*;

/**
 * {@link List} based set of {@link Value} objects. Typically corresponds to a json array structure.
 *
 */
public class ValueList extends Value<List<Value<?>>> implements List<Value<?>> {
    public ValueList() {
        super(new ArrayList());
    }

    /**
     * Creates a new ValueList, while converting and adding the array of raw objects
     * passed as input (through the {@link Value#convert(Object)} method.
     * @param rawItems
     */
    public ValueList(Object... rawItems) {
        this();
        for (Object listItem : rawItems) {
            add(convert(listItem));
        }
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeStartArray(value.size());

        int num = value.size();
        for (int i = 0; i < num; i++) {
            Object obj = value.get(i);
            if (obj instanceof Value<?>) {
                Value<?> cfi = (Value<?>) obj;
                cfi.print(generator);
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
     * @param <T>
     * @return
     */
    public <T> List<T> rawList() {
        List<Value<?>> values = getValue();
        List<T> list = new ArrayList();
        values.forEach(value -> list.add((T)value.getValue()));
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
    public Value<?> merge(Value<?> withValue) {
        // TODO Auto-generated method stub; yet to be implemented, but not sure if array merges can be done / need be done
        return withValue;
    }
}
