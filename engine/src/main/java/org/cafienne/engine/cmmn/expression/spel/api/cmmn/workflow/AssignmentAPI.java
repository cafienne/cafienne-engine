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

package org.cafienne.engine.cmmn.expression.spel.api.cmmn.workflow;

import org.cafienne.engine.cmmn.definition.extension.workflow.AssignmentDefinition;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.constraint.PlanItemRootAPI;
import org.cafienne.engine.cmmn.instance.task.humantask.HumanTask;

public class AssignmentAPI extends PlanItemRootAPI {
    public AssignmentAPI(AssignmentDefinition constraintDefinition, HumanTask task) {
        super(constraintDefinition, task);
    }

    @Override
    public String getDescription() {
        return "assignment expression";
    }
}
