package org.cafienne.platform;

import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.cafienne.akka.actor.handler.AkkaSystemMessageHandler;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.platform.CaseUpdate;
import org.cafienne.cmmn.akka.command.platform.PlatformUpdate;
import org.cafienne.cmmn.akka.command.platform.TenantUpdate;
import org.cafienne.platform.akka.command.PlatformCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class PlatformService extends ModelActor<PlatformCommand, ModelEvent> {
    private final static Logger logger = LoggerFactory.getLogger(PlatformService.class);
    public static final String CAFIENNE_PLATFORM_SERVICE = "cafienne-platform-service";
    private PlatformStorage storage = new PlatformStorage();
    private BlockingQueue<InformJob> jobs = new SynchronousQueue();
    private BlockingQueue<Boolean> hasChanges = new SynchronousQueue();

    public PlatformService() {
        super(PlatformCommand.class, ModelEvent.class);
        setEngineVersion(CaseSystem.version());
        setLastModified(Instant.now());

        // TODO: this thread runs continuously; should be started only when there is something to be saved!
        addPeriodicSnapshotSaver();

        // A thread that nicely awaits work
        startJobHandler();
    }

    @Override
    public String persistenceId() {
        return CAFIENNE_PLATFORM_SERVICE;
    }

    @Override
    protected AkkaSystemMessageHandler createAkkaSystemMessageHandler(Object message) {
        // Typically invoked upon succesful snapshot saving.

        if (message instanceof SaveSnapshotFailure) {
            SaveSnapshotFailure failure = (SaveSnapshotFailure) message;
            // How to go about this?
            logger.error("PLATFORM SERVICE ERROR: Could not save snapshot for platform service", failure.cause());
        } else if (message instanceof SaveSnapshotSuccess){
            refreshStorageObject(this.storage);
        }
        return super.createAkkaSystemMessageHandler(message);
    }

    @Override
    public String getParentActorId() {
        return "";
    }

    @Override
    public String getRootActorId() {
        return getId();
    }

    @Override
    public TransactionEvent createTransactionEvent() {
        return null;
    }

    @Override
    protected void enableSelfCleaner() {
        // Make sure PlatformService remains in memory and is not removed each and every time
    }

    @Override
    protected void handleRecovery(Object event) {
        if (event instanceof SnapshotOffer) {
            SnapshotOffer offer = (SnapshotOffer) event;
            Object snapshot = offer.snapshot();
            if (snapshot instanceof PlatformStorage) {
                refreshStorageObject((PlatformStorage) snapshot);
            }
        } else {
            super.handleRecovery(event);
        }
    }

    private void refreshStorageObject(PlatformStorage storage) {
        this.storage = storage;
        System.out.println("CLEARING " + jobs.size() +" jobs !!!");
        jobs.clear();
        new Thread(() -> {
            System.out.println("Running new thread to fill queue");
            storage.getJobs().forEach(job -> {
                System.out.println("Adding " + job);
                try {
                    jobs.put(job);
                    System.out.println("Added " + job);
                } catch (InterruptedException e) {
                    System.out.println("Failure while adding " + job);
                    e.printStackTrace();
                }
            });
            System.out.println("Completed thread that filled queue");
        }).start();
    }

    void savePlatformStorage(String msg) {
        if (storage.changed()) {
            logger.debug("Storage changed, saving snapshot " + msg);
            saveSnapshot(storage);
            storage.saved();
        } else {
            logger.debug("Store was not changed and will not be saved " + msg);
        }
    }

    /**
     * Adds a thread that will check every now and then whether the timers have changed, and if so stores the snapshot.
     */
    private void addPeriodicSnapshotSaver() {
        Runnable saveJob = () -> {
            try {
                while (true) {
                    FiniteDuration duration = Duration.create(CaseSystem.config().timerService().persistDelay(), TimeUnit.SECONDS);
                    Thread.sleep(duration.toMillis());
                    savePlatformStorage("after period of " + duration);
                }
            } catch (InterruptedException e) {
                // Got interrupted. return
                logger.debug("Received an interrupt; returning");
            }
        };
        new Thread(saveJob).start();
    }

    private void startJobHandler() {
        Runnable jobHandler = () -> {
            try {
                while (true) {
                    System.out.println("Awaiting new job...");
                    InformJob job = jobs.take();
                    System.out.println("Received job " + job);
                    ModelCommand command = job.getCommand();
                    if (command != null) {
                        super.askModel(command, left -> {
                            System.out.println("Failure while sending command to actor! " + left.exception());
                        }, right -> {
                            if (job instanceof InformCaseJob) {
                                storage.removeCase(job.action());
                            } else {
                                storage.removeTenant(job.action());
                            }
                        });
                    }
                }
            } catch (InterruptedException e) {
                // Got interrupted. return
                logger.debug("Received an interrupt; returning");
            }
        };
        new Thread(jobHandler).start();
    }

    @Override
    protected boolean inNeedOfTenantInformation() {
        // No need of tenant information, as this is a singleton actor in this JVM that is tenant-agnostic
        return false;
    }

    public void handleNewInformation(TenantUser user, PlatformUpdate newUserInformation, List<TenantUpdate> tenantsToUpdate, List<CaseUpdate> casesToUpdate) {
        storage.setUser(user);
        storage.setNewInformation(newUserInformation);
        tenantsToUpdate.forEach(storage::add);
        casesToUpdate.forEach(storage::add);
        savePlatformStorage("Received new information to handle");
    }
}