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
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.platform.CaseUpdate;
import org.cafienne.cmmn.akka.command.platform.NewUserInformation;
import org.cafienne.cmmn.akka.command.platform.PlatformUpdate;
import org.cafienne.cmmn.akka.command.platform.TenantUpdate;
import org.cafienne.platform.akka.command.PlatformCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class PlatformService extends ModelActor<PlatformCommand, ModelEvent> {
    private final static Logger logger = LoggerFactory.getLogger(PlatformService.class);
    public static final String CAFIENNE_PLATFORM_SERVICE = "cafienne-platform-service";
    private PlatformStorage storage = new PlatformStorage();
    private BlockingQueue<InformJob> jobs = new LinkedBlockingQueue<>();

    public PlatformService() {
        super(PlatformCommand.class, ModelEvent.class);
        setEngineVersion(CaseSystem.version());
        setLastModified(Instant.now());
        addPeriodicSnapshotSaver();
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
            System.out.println("Received system message of type " + message.getClass().getName());
            System.out.println("CLEARING " + jobs.size() +" jobs !!!");
            jobs.clear();
            storage.getJobs().forEach(job -> {
                System.out.println("Adding job " + job);
                jobs.add(job);
            });
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
//        storage.getTimers().forEach(this::setTimer);
    }

    void saveTimerStorage(String msg) {
        if (storage.changed()) {
            System.out.println("Saving snapshot " + msg);
            logger.debug("Storage changed, saving snapshot " + msg);
            saveSnapshot(storage);
            storage.saved();
            System.out.println("Saved storage");
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
                    saveTimerStorage("after period of " + duration);
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
                    InformJob job = jobs.take();
                    System.out.println("Got a job on " + job);
                    ModelCommand command = job.getCommand();
                    if (command != null) {
                        System.out.println("Sending command of type " + command.getClass().getSimpleName());
                        super.askModel(command, left -> {
                            System.out.println("Failure while sending command to actor! " + left.exception());
                        }, right -> {
                            System.out.println("Got a right response, removing job of type " + job.getClass().getSimpleName());
                            if (job instanceof InformCaseJob) {
                                System.out.println("Removing case job");
                                storage.removeCase(job.getActorId());
                            } else {
                                System.out.println("Removing tenant job for tenant " + job.getActorId());
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
        saveTimerStorage("Received new information to handle");
    }
}