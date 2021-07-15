package org.cafienne.cmmn.definition.task.validation;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.validation.TaskOutputValidator;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.http.HTTPCallDefinition;

public class TaskOutputValidatorDefinition extends CMMNElementDefinition {
    private final ProcessDefinition processDef;
    private final HTTPCallDefinition httpDefinition;

    public TaskOutputValidatorDefinition(ProcessDefinition definition) {
        super(definition.getElement(), definition, definition);
        this.processDef = definition;
        if (!(this.processDef.getImplementation() instanceof HTTPCallDefinition)) {
            definition.addDefinitionError("Task validator " + definition + "");
        }
        this.httpDefinition = (HTTPCallDefinition) this.processDef.getImplementation();
    }

    public TaskOutputValidator createInstance(HumanTask task) {
        return this.httpDefinition.createValidator(task);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameValidator);
    }

    public boolean sameValidator(TaskOutputValidatorDefinition other) {
        return same(processDef, other.processDef)
                && same(httpDefinition, other.httpDefinition);
    }
}
