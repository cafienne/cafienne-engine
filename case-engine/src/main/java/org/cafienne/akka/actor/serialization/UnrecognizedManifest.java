package org.cafienne.akka.actor.serialization;

import java.io.Serializable;

/**
 * Wrapper for events with outdated or too new manifests (i.e. {@link AkkaCaseObjectSerializer} does not recognize them.
 */
public class UnrecognizedManifest implements Serializable {
    public final String manifest;
    public final byte[] blob;

    UnrecognizedManifest(String manifest, byte[] blob) {
        this.manifest = manifest;
        this.blob = blob;
    }

    @Override
    public String toString() {
        return "Unrecognized manifest "+manifest+" with blob "+new String(blob);
    }
}
