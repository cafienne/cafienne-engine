/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;

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
        this.owner = readField(json, Fields.owner);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
        writeField(generator, Fields.owner, owner);
    }

    @Override
    public void updateState(Case caseInstance) {
        getTask().getImplementation().updateState(this);
    }
}
