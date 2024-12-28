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
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.expression.spel.SpelReadable;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * NullValue is an object to prevent NullPointerExceptions
 */
class NullValue extends PrimitiveValue<Object> implements SpelReadable, List<Object> {
    NullValue() {
        super(null);
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeNull();
    }

    @Override
    public NullValue cloneValueNode() {
        return this; // singleton only??
    }

    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        return true; // Null matches all types
    }

    @Override
    public boolean isSupersetOf(Value<?> otherValue) {
        return otherValue == null || otherValue.value == null;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public Value<?> read(String propertyName) {
        // Anything can be read from the Null object, but it is always returning a new Null object
        return this;
    }

    @Override
    public boolean canRead(String propertyName) {
        return true;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<Object> iterator() {
        final NullValue self = this;
        return new Iterator() {
            @Override
            public Object next() {
                return self;
            }

            @Override
            public boolean hasNext() {
                return false;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return a;
    }

    @Override
    public boolean add(Object e) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends Object> c) {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Object> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public Object get(int index) {
        return this;
    }

    @Override
    public Object set(int index, Object element) {
        return null;
    }

    @Override
    public void add(int index, Object element) {
    }

    @Override
    public Object remove(int index) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return -1;
    }

    @Override
    public ListIterator<Object> listIterator() {
        NullValue self = this;
        return new ListIterator() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
                return self;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Object previous() {
                return self;
            }

            @Override
            public int nextIndex() {
                return 0;
            }

            @Override
            public int previousIndex() {
                return 0;
            }

            @Override
            public void remove() {
            }

            @Override
            public void set(Object e) {
            }

            @Override
            public void add(Object e) {
            }
        };
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return listIterator();
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return this;
    }
}