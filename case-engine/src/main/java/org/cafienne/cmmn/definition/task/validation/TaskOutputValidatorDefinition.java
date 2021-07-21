package org.cafienne.cmmn.definition.task.validation;

import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.validation.TaskOutputValidator;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.http.HTTPCallDefinition;

public class TaskOutputValidatorDefinition {
    private final ProcessDefinition processDef;
    private final HTTPCallDefinition httpDefinition;

    public TaskOutputValidatorDefinition(ProcessDefinition definition) {
        this.processDef = definition;
        if (!(this.processDef.getImplementation() instanceof HTTPCallDefinition)) {
            definition.addDefinitionError("Task validator " + definition + "");
        }
        this.httpDefinition = (HTTPCallDefinition) this.processDef.getImplementation();
    }

    public TaskOutputValidator createInstance(HumanTask task) {
        return this.httpDefinition.createValidator(task);
    }
}
