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

package com.casefabric.cmmn.instance.parameter;

import com.casefabric.cmmn.definition.parameter.ParameterDefinition;
import com.casefabric.cmmn.instance.Parameter;
import com.casefabric.cmmn.instance.Task;
import com.casefabric.json.Value;

/**
 * TaskParameter is specific to {@link Task} input and output
 */
public class TaskParameter<P extends ParameterDefinition> extends Parameter<P> {
    protected final Task<?> task;

    protected TaskParameter(P definition, Task<?> task, Value<?> value) {
        super(definition, task.getCaseInstance(), value);
        this.task = task;
    }
}
