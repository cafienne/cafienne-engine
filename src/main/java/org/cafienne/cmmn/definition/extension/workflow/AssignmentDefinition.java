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

package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.w3c.dom.Element;

public class AssignmentDefinition extends ConstraintDefinition {
    public AssignmentDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    public String getContextDescription() {
        String parentType = getParentElement().getType();
        String parentId = getParentElement().getId();
        // This will return something like "The required rule in HumanTask 'abc'
        return "The " + getType() + " in " + parentType + " '" + parentId + "'";
    }

    public String evaluate(HumanTask task) {
        return getExpressionDefinition().getEvaluator().evaluateAssignee(task, this);
    }

    /**
     * Returns the type of constraint, e.g. applicabilityRule, ifPart, repetitionRule, etc.
     *
     * @return
     */
    public String getType() {
        return "assignment";
    }
}