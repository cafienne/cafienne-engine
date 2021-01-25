package org.cafienne.akka.actor.serialization;

import org.cafienne.platform.PlatformStorage;
import org.cafienne.timerservice.TimerStorage;

public class SnapshotSerializer extends CafienneSerializer {
    static void register() {
        registerSnapshots();
    }

    private static void registerSnapshots() {
        addManifestWrapper(TimerStorage.class, TimerStorage::new);
        addManifestWrapper(PlatformStorage.class, PlatformStorage::new);
    }
}
