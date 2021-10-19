package org.cafienne.actormodel.snapshot;

import org.cafienne.actormodel.ModelActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Relaxed snapshot can be used to set a timer to save the snapshot
 * no on immediate changes, but after a certain period of time.
 * This can be used if the snapshot may have to be updated in a high frequency
 */
public abstract class RelaxedSnapshot<M extends ModelActor> implements ModelActorSnapshot {
    private final static Logger logger = LoggerFactory.getLogger(RelaxedSnapshot.class);
    private final M actor;
    private final FiniteDuration delay;
    private TimedSnapshotSaver timedSnapshotSaver = null;

    protected RelaxedSnapshot(M actor, FiniteDuration delay) {
        this.actor = actor;
        this.delay = delay;
    }

    protected RelaxedSnapshot() {
        this.actor = null;
        this.delay = null;
    }

    /**
     * Adds a thread that will check every now and then whether the timers have changed, and if so stores the snapshot.
     */
    protected void enableTimedSnapshotSaver() {
        synchronized (this) {
            if (timedSnapshotSaver != null) {
                timedSnapshotSaver.changes++;
                return;
            }
            getLogger().debug("Saving snapshot in " + delay);
            timedSnapshotSaver = new TimedSnapshotSaver();
            timedSnapshotSaver.start();
        }
    }

    protected void save(String msg) {
        synchronized (this) {
            getLogger().debug("Storage changed, saving snapshot - " + msg);
            if (timedSnapshotSaver != null) {
                timedSnapshotSaver.aborted = true;
                timedSnapshotSaver = null;
            }
            actor.saveSnapshot(this);
        }
    }

    protected Logger getLogger() {
        return logger;
    }

    class TimedSnapshotSaver extends Thread {
        int changes = 1;
        boolean aborted = false;

        @Override
        public void run() {
            try {
                Thread.sleep(delay.toMillis());
                if (! aborted) {
                    save("after delay of " + delay +" in which " + changes +" changes occurred");
                }
            } catch (InterruptedException e) {
                // Got interrupted. return
                getLogger().debug("Received an interrupt; returning");
            }
        }
    }
}
