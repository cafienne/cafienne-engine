package org.cafienne.cmmn.actorapi.command.plan.task;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.cmmn.actorapi.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.akka.actor.serialization.json.ValueMap;

@Manifest
public class FailTask extends CompleteTask {
    public FailTask(ModelActor child, ValueMap taskOutput) {
        super(child, taskOutput);
    }

    public FailTask(ValueMap json) {
        super(json);
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        task.goFault(taskOutput);
        return new CaseResponse(this);
    }
}
