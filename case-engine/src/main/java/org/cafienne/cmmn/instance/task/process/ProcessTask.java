/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.task.process;

import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.processtask.akka.command.*;

public class ProcessTask extends Task<ProcessTaskDefinition> {
    private final ProcessInformer informer;
    public ProcessTask(String id, int index, ItemDefinition itemDefinition, ProcessTaskDefinition definition, Stage stage) {
        super(id, index, itemDefinition, definition, stage);
        informer = ProcessInformer.getInstance(this, definition);
    }

    @Override
    protected void createInstance() {
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        informer.startImplementation(inputParameters);
    }

    @Override
    protected void suspendInstance() {
        informer.suspendInstance();
    }

    @Override
    protected void resumeInstance() {
        informer.resumeInstance();;
    }

    @Override
    protected void terminateInstance() {
        informer.terminateInstance();
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        informer.reactivateImplementation(inputParameters);
    }
}
