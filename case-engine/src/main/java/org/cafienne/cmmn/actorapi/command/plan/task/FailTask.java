package org.cafienne.cmmn.actorapi.command.plan.task;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.actormodel.serialization.json.ValueMap;

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
