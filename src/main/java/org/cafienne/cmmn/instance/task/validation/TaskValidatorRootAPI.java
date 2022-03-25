package org.cafienne.cmmn.instance.task.validation;

import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.json.ValueMap;

public class TaskValidatorRootAPI extends CaseRootObject {
    public TaskValidatorRootAPI(HumanTask task, ValueMap requestPayloadJson) {
        super(task.getCaseInstance());
        requestPayloadJson.getValue().forEach(this::addProperty);
    }

    @Override
    public String getDescription() {
        return "Task validation";
    }
}
