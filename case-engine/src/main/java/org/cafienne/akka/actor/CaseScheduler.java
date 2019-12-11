package org.cafienne.akka.actor;

import akka.actor.Cancellable;
import org.cafienne.util.Guid;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple wrapper to manage jobs that run asynchronously inside a case.
 * This wrapper is used to be able to clean up jobs when the case crashes (i.e., already created events
 * because of state changes and also ran into an error causing to undo those changes; if that happens,
 * the Case.java invokes Actor.supervisor.restartChild(this), but it means we also need to cleanup the jobs).
 */
public class CaseScheduler {
    private final ModelActor actor;
    private final akka.actor.Scheduler akkaScheduler;
    private final Map<String, Cancellable> jobs = new HashMap<>();
    private static int instanceCounter = 1;
    private final int instance = instanceCounter++;

    /**
     * Simple wrapper to manage jobs that run asynchronously inside the case.
     * @param actor
     */
    CaseScheduler(ModelActor actor) {
        this.actor = actor;
        this.akkaScheduler = actor.getContext().system().scheduler();
    }

    /**
     * Cancels all schedules jobs and removes them.
     */
    public void clearSchedules() {
        jobs.values().forEach(job -> {
//            println("Removing a job");
            job.cancel();
        });
        jobs.clear();
    }

    private int i = 0;

    public Cancellable schedule(FiniteDuration duration, Runnable job) {
        final int jobInstance  = i++;
        // Generate an id for the work to be done, so that we can keep track of it
        final String jobId = new Guid().toString();
//        println("\n\nScheduling job "+ jobInstance);
        // Create a small wrapper such that we can remove the job before executing it.
        final Runnable jobWrapper = () -> {
//            println("Running job "+jobInstance);
            jobs.remove(jobId);
            job.run();
        };

        // Use akka's scheduler to get an actual thread to do the job.
        Cancellable worker = akkaScheduler.scheduleOnce(duration, jobWrapper, actor.context().system().dispatcher());

        // Store the job such that we can remove it upon case crashes...
        this.jobs.put(jobId, worker);

        // Hand back the job such that the invoker can also cancel the job.
        return new Cancellable() {
            @Override
            public boolean cancel() {
//                println("Removing job " + jobInstance);
                // Make sure that we also update the administration if the job is canceled by the invoker.
                jobs.remove(jobId);
                return worker.cancel();
            }

            @Override
            public boolean isCancelled() {
                return worker.isCancelled();
            }
        };
    }

    /**
     * Debugging method.
     * @param msg
     */
    public void println(String msg) {
//        System.out.println("Scheduler "+ instance +": "+msg);
    }
}
