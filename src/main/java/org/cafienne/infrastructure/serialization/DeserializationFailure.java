package org.cafienne.infrastructure.serialization;

import java.io.Serializable;

/**
 * Wrapper for events with outdated or too new manifests (i.e. {@link CafienneSerializer} does not recognize them.
 */
public class DeserializationFailure implements Serializable {
    public final String manifest;
    public final Exception exception;
    public final byte[] blob;
    private final String msg;

    public DeserializationFailure(String manifest, DeserializationError e, byte[] blob) {
        this.manifest = manifest;
        this.exception = e;
        this.blob = blob;
        this.msg = "Fatal error in deserializing manifest " + manifest + "\nMessage: " + exception.getMessage() + "\nEvent blob: " + new String(blob);
    }

    public DeserializationFailure(String manifest, Exception e, byte[] blob) {
        this.manifest = manifest;
        this.exception = e;
        this.blob = blob;
        this.msg = "Fatal error in deserializing manifest " + manifest + "\n" + exception + "\nEvent blob: " + new String(blob);
    }

    @Override
    public String toString() {
        return msg;
    }
}
