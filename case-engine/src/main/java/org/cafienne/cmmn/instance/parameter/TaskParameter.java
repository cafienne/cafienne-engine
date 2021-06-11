/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.parameter;

import org.cafienne.json.Value;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.instance.Parameter;
import org.cafienne.cmmn.instance.Task;

/**
 * TaskParameter is specific to {@link Task} input and output
 */
public class TaskParameter<P extends ParameterDefinition> extends Parameter<P> {
    protected final Task task;

    protected TaskParameter(P definition, Task task, Value value) {
        super(definition, task.getCaseInstance(), value);
        this.task = task;
    }
}
