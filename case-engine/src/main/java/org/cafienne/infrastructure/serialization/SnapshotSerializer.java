package org.cafienne.infrastructure.serialization;

import org.cafienne.platform.PlatformStorage;
import org.cafienne.timerservice.TimerStorage;

public class SnapshotSerializer extends CafienneSerializer {
    public static void register() {
        registerSnapshots();
    }

    private static void registerSnapshots() {
        addManifestWrapper(TimerStorage.class, TimerStorage::new);
        addManifestWrapper(PlatformStorage.class, PlatformStorage::new);
    }
}
