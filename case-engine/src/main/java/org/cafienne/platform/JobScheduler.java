package org.cafienne.platform;

import org.cafienne.actormodel.config.Cafienne;
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
            int numWorkers = Cafienne.config().engine().platformServiceConfig().workers();
            while (numWorkers-- > 0) {
                new JobRunner(service, this, numWorkers + 1).start(); // +1 because that is a little more clear when reading the log messages
            }
        }

        // Now schedule pending updates one at a time as a job (if there are any).
        //  Note that storage.getBatches is expected to ony give new batches, otherwise we keep sending same job over and over again.
        List<BatchJob> newBatchJobs = storage.getNewBatches();
        List<InformJob> jobs = new ArrayList();
        newBatchJobs.forEach(batch -> jobs.addAll(batch.getJobs()));
        if (jobs.isEmpty()) {
            return;
        }
        // Start a new thread to put the pending jobs into the queue, to avoid blocking this thread
        //  Note:
        new Thread(() -> jobs.forEach(job -> {
            try {
                jobQueue.put(job);
            } catch (InterruptedException e) {
                logger.warn("Scheduling next pending job was interrupted (perhaps shutting down?)", e);
            }
        })).start();
    }

    /**
     * Invoked by JobRunners to get the next job to run
     *
     * @return
     * @throws InterruptedException
     */
    InformJob getNewJob() throws InterruptedException {
        return jobQueue.take();
    }
}
