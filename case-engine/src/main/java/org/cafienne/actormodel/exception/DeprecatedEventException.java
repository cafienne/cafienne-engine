package org.cafienne.actormodel.exception;

/**
 * Exception that can be raised to avoid having to set final fields in a deprecated event,
 * whilst still keeping those for need of deserialization if they were generated in the past.
 */
public class DeprecatedEventException extends IllegalArgumentException {
    public DeprecatedEventException() {
        super("This type of event is no longer supported; only maintained for backwards compatibility");
    }
}
