package org.cafienne.json;

/**
 * Supports parsing a ValueMap to an instance of type T
 * @param <T> The target type of parsing the map.
 */
@FunctionalInterface
public interface ValueMapParser<T> {
    T convert(ValueMap json);
}
