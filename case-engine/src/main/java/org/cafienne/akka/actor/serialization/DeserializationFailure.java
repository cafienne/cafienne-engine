package org.cafienne.akka.actor.serialization;

import java.io.Serializable;

/**
 * Wrapper for events with outdated or too new manifests (i.e. {@link AkkaCaseObjectSerializer} does not recognize them.
 */
public class DeserializationFailure implements Serializable {
    public final String manifest;
    public final Exception exception;
    public final byte[] blob;

    DeserializationFailure(String manifest, Exception e, byte[] blob) {
        this.manifest = manifest;
        this.exception = e;
        this.blob = blob;
    }

    @Override
    public String toString() {
        return "Unrecognized manifest "+exception+" with blob "+new String(blob);
    }
}
