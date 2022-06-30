/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class TaskImplementationStarted extends TaskEvent<Task<?>> {
    public TaskImplementationStarted(Task<?> task) {
        super(task);
    }

    public TaskImplementationStarted(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(Task<?> task) {
        // Just for logging purposes, no actual state change
        task.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTaskEvent(generator);
    }
}
