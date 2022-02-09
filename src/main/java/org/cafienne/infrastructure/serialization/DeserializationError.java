package org.cafienne.infrastructure.serialization;

/**
 * This exception is thrown when a json structure cannot be deserialized to an object structure.
 *
 */
public class DeserializationError extends RuntimeException {
    public DeserializationError(String msg) {
        super(msg);
    }

    public DeserializationError(String msg, Throwable cause) {
        super(msg, cause);
    }
}
