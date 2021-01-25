package org.cafienne.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

class JobRunner extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(JobRunner.class);
    private final PlatformService service;
    private final JobScheduler jobScheduler;
    private final int identifier;
    private final BlockingQueue<Boolean> availability = new SynchronousQueue();

    JobRunner(PlatformService service, JobScheduler jobScheduler, int identifier) {
        this.service = service;
        this.jobScheduler = jobScheduler;
        this.identifier = identifier;
    }

    void log(String msg) {
        logger.debug("JobRunner[" + identifier + "]: " + msg);
//        System.out.println("JobRunner[" + identifier + "]: " + msg);
    }

    @Override
    public void run() {
        try {
            while (true) {
                log("Awaiting new job...");
                InformJob job = jobScheduler.getNewJob();
                job.run(service, this);
                log("Blocking until new job is finished");
                availability.put(false);
            }
        } catch (InterruptedException e) {
            // Got interrupted. return
            logger.warn("Job Handler [" + identifier + "] received an interrupt while making itself available again and is no longer active", e);
        }
    }

    void finished(InformJob job) {
        log("Finished job " + job + ", making myself available again");
        try {
            availability.take();
        } catch (InterruptedException e) {
            logger.warn("Job Handler was interrupted (perhaps shutting down?)", e);
        }
    }
}
