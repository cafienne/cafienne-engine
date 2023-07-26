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
        return getImplementationAttribute("required");
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
