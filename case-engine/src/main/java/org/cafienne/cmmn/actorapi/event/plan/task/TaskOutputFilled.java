/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class TaskOutputFilled extends TaskEvent<Task<?>> {
    private final ValueMap parameters;
    private final ValueMap rawOutputParameters;

    public TaskOutputFilled(Task<?> task, ValueMap outputParameters, ValueMap rawOutputParameters) {
        super(task);
        this.parameters = outputParameters;
        this.rawOutputParameters = rawOutputParameters;
    }

    public TaskOutputFilled(ValueMap json) {
        super(json);
        this.parameters = readMap(json, Fields.parameters);
        this.rawOutputParameters = readMap(json, Fields.rawOutputParameters);
    }

    @Override
    public void updateState(Case caseInstance) {
        getTask().updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTaskEvent(generator);
        writeField(generator, Fields.parameters, parameters);
        writeField(generator, Fields.rawOutputParameters, rawOutputParameters);
    }

    @Override
    public String toString() {
        return "Plan item " + getTaskId() + " has output:\n" + parameters;
    }

    public ValueMap getTaskOutputParameters() {
        return parameters;
    }

    public ValueMap getRawOutputParameters() {
        return rawOutputParameters;
    }

}
