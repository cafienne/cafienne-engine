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

package org.cafienne.cmmn.definition.extension.workflow.validation;

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
