/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class HumanTaskOwnerChanged extends HumanTaskEvent {
    /**
     * New owner of the task
     */
    public final String owner; // new owner of the task


    public HumanTaskOwnerChanged(HumanTask task, String owner) {
        super(task);
        this.owner = owner;
    }

    public HumanTaskOwnerChanged(ValueMap json) {
        super(json);
        this.owner = json.readString(Fields.owner);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
        writeField(generator, Fields.owner, owner);
    }

    @Override
    public void updateState(WorkflowTask task) {
        super.updateState(task);
        task.updateState(this);
    }
}
