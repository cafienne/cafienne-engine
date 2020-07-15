package org.cafienne.akka.actor.serialization;

import org.cafienne.timerservice.TimerStorage;

public class SnapshotSerializer extends AkkaCaseObjectSerializer {
    /**
     * The unique identifier for the AkkaCaseObjectSerializer (value is <code>424242</code>)
     */
    public static final int IDENTIFIER = 52932027;

    @Override
    public int identifier() {
        return IDENTIFIER;
    }

    static {
        registerSnapshots();
    }

    private static void registerSnapshots() {
        addManifestWrapper(TimerStorage.class, TimerStorage::new);
    }
}
