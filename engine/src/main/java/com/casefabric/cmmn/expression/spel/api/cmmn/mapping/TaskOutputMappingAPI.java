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

package com.casefabric.cmmn.expression.spel.api.cmmn.mapping;

import com.casefabric.cmmn.definition.parameter.ParameterDefinition;
import com.casefabric.cmmn.instance.Task;
import com.casefabric.json.Value;

/**
 * Provides context for input/output transformation of parameters.
 * Can read the parameter name in the expression and resolve it to the parameter value.
 * Contains furthermore a task property, to provide for the task context for which this parameter transformation is being executed.
 */
public class TaskOutputMappingAPI extends TaskMappingAPI {
    private final ParameterDefinition from;
    private final ParameterDefinition to;

    public TaskOutputMappingAPI(ParameterDefinition from, ParameterDefinition to, Value<?> value, Task<?> task) {
        super(from.getName(), value, task);
        this.from = from;
        this.to = to;
    }

    @Override
    public String getDescription() {
        return "mapping raw output parameter '" + from.getName() + "' to task output parameter '" + to.getName() + "'";
    }
}
