package org.cafienne.platform;

import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.cmmn.akka.command.UpdateCaseWithPlatformInformation;
import org.cafienne.platform.akka.command.UpdatePlatformInformation;
import org.cafienne.tenant.akka.command.platform.UpdateTenantWithPlatformInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

class JobScheduler {
    private final static Logger logger = LoggerFactory.getLogger(JobScheduler.class);
    private final PlatformService service;
    private final PlatformStorage storage;
    private final BlockingQueue<InformJob> jobQueue = new SynchronousQueue();
    private boolean awake = false;

    public JobScheduler(PlatformService service, PlatformStorage storage) {
        this.service = service;
        this.storage = storage;
    }

    void wakeUp() {
        if (!awake) {
            awake = true;
            // Start 5 threads that nicely await work
            int numWorkers = CaseSystem.config().engine().platformServiceConfig().workers();
            while (numWorkers-- > 0) {
                new JobRunner(service, this, numWorkers + 1).start(); // +1 because that is a little more clear when reading the log messages
            }
        }

        // Now schedule pending updates one at a time as a job (if there are any).
        //  Note that storage.getPendingUpdates is expected to clear the list, otherwise we keep sending same job over and over again.
        List<UpdatePlatformInformation> pendingUpdates = storage.getPendingUpdates();
        if (pendingUpdates.isEmpty()) {
            return;
        }

        final List<InformJob> pendingJobs = new ArrayList<>();
        // Convert PlatformUpdates into InformJobs
        new ArrayList<>(pendingUpdates).forEach(command -> {
            PlatformUser user = PlatformUser.from(command.getUser());
            command.tenants.forEach(update -> pendingJobs.add(new InformJob(storage, command.tenants, update, new UpdateTenantWithPlatformInformation(user, update))));
            command.cases.forEach(update -> pendingJobs.add(new InformJob(storage, command.cases, update, new UpdateCaseWithPlatformInformation(user, update))));
        });

        // Start a new thread to put the pending jobs into the queue, to avoid blocking this thread
        //  Note:
        new Thread(() -> pendingJobs.forEach(job -> {
            try {
                jobQueue.put(job);
            } catch (InterruptedException e) {
                logger.warn("Scheduling next pending job was interrupted (perhaps shutting down?)", e);
            }
        })).start();
    }

    /**
     * Invoked by JobRunners to get the next job to run
     * @return
     * @throws InterruptedException
     */
    InformJob getNewJob() throws InterruptedException {
        return jobQueue.take();
    }
}
