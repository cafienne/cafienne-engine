/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.parameter;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.TaskDefinition;
import org.w3c.dom.Element;

public class TaskOutputParameterDefinition extends OutputParameterDefinition {

    /**
     * The engine provides an extension to task output parameters; they can be indicated to be mandatory.
     * The effect of setting this flag to true on a parameter is that it is not possible to complete a task when the value of the parameter still have a null value.
     */
    private final boolean isMandatory;

    public TaskOutputParameterDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        isMandatory = getMandatoryValue();
    }

    /**
     * Returns the value of the extension element with the mandatory attribute.
     *
     * @return
     */
    private boolean getMandatoryValue() {
        Element extension = getExtension("implementation", false);
        if (extension == null) {
            return false;
        }
        String isRequired = extension.getAttribute("required");
        if (isRequired.equalsIgnoreCase("true")) {
            return true;
        } else if (isRequired.isEmpty() || isRequired.equalsIgnoreCase("false")) {
            return false;
        } else {
            TaskDefinition<?> task = getParentElement();
            getModelDefinition().addDefinitionError("Output parameter " + getName() + " in task " + task.getName() + " has an invalid value for the required attribute: '" + isRequired + "'");
            return false;
        }
    }

    /**
     * Returns true if an extension element is defined on the task that has the attribute 'required' set to true (case insensitive)
     * It may be interpreted as 'tasks of this type cannot complete if the parameter is not filled with a non-null value'.
     *
     * @return
     */
    public boolean isMandatory() {
        return isMandatory;
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameTaskParameter);
    }

    public boolean sameTaskParameter(TaskOutputParameterDefinition other) {
        return sameParameter(other)
                && same(isMandatory, other.isMandatory);
    }
}
