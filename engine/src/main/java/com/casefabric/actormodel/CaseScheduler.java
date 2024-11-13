/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.actormodel;

import org.apache.pekko.actor.Cancellable;
import com.casefabric.util.Guid;
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
    private final org.apache.pekko.actor.Scheduler systemScheduler;
    private final Map<String, Cancellable> jobs = new HashMap<>();

    /**
     * Simple wrapper to manage jobs that run asynchronously inside the case.
     * @param actor
     */
    CaseScheduler(ModelActor actor) {
        this.actor = actor;
        this.systemScheduler = actor.getContext().system().scheduler();
    }

    /**
     * Cancels all schedules jobs and removes them.
     */
    public void clearSchedules() {
        jobs.values().forEach(Cancellable::cancel);
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

        // Use system scheduler to get an actual thread to do the job.
        Cancellable worker = systemScheduler.scheduleOnce(duration, jobWrapper, actor.context().system().dispatcher());

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
