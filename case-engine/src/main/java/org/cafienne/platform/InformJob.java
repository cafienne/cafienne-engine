package org.cafienne.platform;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.response.CommandFailure;

import java.util.List;

class InformJob<A extends Object> {
    private final PlatformStorage storage;
    private final List<A> collection;
    final A action;
    private final ModelCommand command;
    private final String type;

    public InformJob(PlatformStorage storage, List<A> collection, A action, ModelCommand command) {
        this.storage = storage;
        this.action = action;
        this.collection = collection;
        this.command = command;
        this.type = this.getClass().getSimpleName();
    }

    void run(PlatformService service, JobRunner jobRunner) {
        jobRunner.log("Running job " + this);
        service.askModel(command, left -> failed(left, jobRunner), right -> succeeded(jobRunner));
    }

    void failed(CommandFailure failure, JobRunner jobRunner) {
        storage.reportFailure(this, failure);
        jobRunner.log("Failure while sending command to actor! " + failure.exception());
        jobRunner.log("Releasing handler for next job");
        jobRunner.finished(this);
    }

    void succeeded(JobRunner jobRunner) {
        collection.remove(action);
        jobRunner.log("Completed job " + this + ". Remaining job collection has " + collection.size() + " elements");
        storage.reportSuccess(this);
        jobRunner.finished(this);
    }

    @Override
    public String toString() {
        return "Job to run " + action.getClass().getSimpleName() + " on " + command.getActorId() + " (in tenant " + command.getUser().tenant() + ") about new users";
    }
}
