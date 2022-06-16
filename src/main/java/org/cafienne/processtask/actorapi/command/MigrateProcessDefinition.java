/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessDefinitionMigrated;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class MigrateProcessDefinition extends ProcessCommand {
    private final ProcessDefinition newDefinition;

    /**
     * Migrate the definition of a case.
     *
     * @param caseInstanceId The instance identifier of the case
     * @param newDefinition  The case definition (according to the CMMN xsd) to be updated to
     */
    public MigrateProcessDefinition(UserIdentity user, String caseInstanceId, ProcessDefinition newDefinition) {
        super(user, caseInstanceId);
        this.newDefinition = newDefinition;
    }

    public MigrateProcessDefinition(ValueMap json) {
        super(json);
        this.newDefinition = json.readDefinition(Fields.definition, ProcessDefinition.class);
    }

    public ProcessDefinition getNewDefinition() {
        return newDefinition;
    }

    @Override
    public String toString() {
        return "Migrate Case Definition '" + newDefinition.getName() + "'";
    }

    @Override
    protected void process(ProcessTaskActor processTaskActor, SubProcess<?> implementation) {
        if (processTaskActor.getDefinition().sameProcessDefinition(newDefinition)) {
            processTaskActor.addDebugInfo(() -> "No need to migrate definition of process task " + processTaskActor.getId() + " (proposed definition already in use)");
        } else if (!processTaskActor.getDefinition().getImplementation().sameType(newDefinition.getImplementation())) {
            processTaskActor.addDebugInfo(() -> "Migration of process task implementation to a different type is not supported");
        } else {
            processTaskActor.addEvent(new ProcessDefinitionMigrated(processTaskActor, this));
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.definition, newDefinition);
    }
}
