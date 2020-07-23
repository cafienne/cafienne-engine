package org.cafienne.akka.actor.serialization;

/**
 * This exception is thrown when a CMMNElementDefinition cannot be deserialized from a JSON file.
 *
 */
public class DeserializationError extends RuntimeException {
    public DeserializationError(String msg, Throwable cause) {
        super(msg, cause);
    }
}
