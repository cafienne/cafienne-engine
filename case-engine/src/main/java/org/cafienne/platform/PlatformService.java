package org.cafienne.platform;

import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.event.TransactionEvent;
import org.cafienne.actormodel.handler.AkkaSystemMessageHandler;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.platform.actorapi.command.GetUpdateStatus;
import org.cafienne.platform.actorapi.command.PlatformCommand;
import org.cafienne.platform.actorapi.command.UpdatePlatformInformation;
import org.cafienne.platform.actorapi.response.PlatformUpdateStatus;
import org.cafienne.system.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class PlatformService extends ModelActor<PlatformCommand, ModelEvent> {
    private final static Logger logger = LoggerFactory.getLogger(PlatformService.class);
    public static final String CAFIENNE_PLATFORM_SERVICE = "cafienne-platform-service";
    private final PlatformStorage storage = new PlatformStorage(this);
    private final JobScheduler jobScheduler = new JobScheduler(this, storage);

    public PlatformService(CaseSystem caseSystem) {
        super(PlatformCommand.class, ModelEvent.class, caseSystem);
        setEngineVersion(Cafienne.version());
        setLastModified(Instant.now());
    }

    @Override
    public String persistenceId() {
        return CAFIENNE_PLATFORM_SERVICE;
    }

    @Override
    protected AkkaSystemMessageHandler createAkkaSystemMessageHandler(Object message) {
        // Typically invoked upon successful snapshot saving.
        if (message instanceof SaveSnapshotFailure) {
            SaveSnapshotFailure failure = (SaveSnapshotFailure) message;
            // How to go about this?
            logger.error("PLATFORM SERVICE ERROR: Could not save snapshot for platform service", failure.cause());
        } else if (message instanceof SaveSnapshotSuccess) {
            logger.info("Platform service snapshot stored successfully");
            jobScheduler.wakeUp();
        }
        return super.createAkkaSystemMessageHandler(message);
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
    protected void recoveryCompleted() {
        // Note: the job schedule should only be woken up upon recovery completion,
        // instead of after successful snapshot offer recovery
        logger.info("Platform service snapshot recovered, waking up job scheduler");
        jobScheduler.wakeUp();
    }

    @Override
    protected void handleRecovery(Object event) {
        if (event instanceof SnapshotOffer) {
            SnapshotOffer offer = (SnapshotOffer) event;
            Object snapshot = offer.snapshot();
            if (snapshot instanceof PlatformStorage) {
                storage.merge((PlatformStorage) snapshot);
             }
        } else {
            super.handleRecovery(event);
        }
    }

    @Override
    protected boolean inNeedOfTenantInformation() {
        // No need of tenant information, as this is a singleton actor in this JVM that is tenant-agnostic
        return false;
    }

    public void handleUpdate(UpdatePlatformInformation updatePlatformInformation) {
        storage.addUpdate(updatePlatformInformation);
    }

    public PlatformUpdateStatus getUpdateStatus(GetUpdateStatus command) {
        return new PlatformUpdateStatus(command, storage.getStatus());
    }
}