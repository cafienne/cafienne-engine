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

package com.casefabric.processtask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;
import com.casefabric.processtask.actorapi.command.MigrateProcessDefinition;
import com.casefabric.processtask.definition.ProcessDefinition;
import com.casefabric.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ProcessDefinitionMigrated extends BaseProcessEvent {
    private final ProcessDefinition newDefinition;

    public ProcessDefinitionMigrated(ProcessTaskActor actor, MigrateProcessDefinition command) {
        super(actor);
        this.newDefinition = command.getNewDefinition();
    }

    public ProcessDefinitionMigrated(ValueMap json) {
        super(json);
        this.newDefinition = json.readDefinition(Fields.definition, ProcessDefinition.class);
    }

    public ProcessDefinition getNewDefinition() {
        return newDefinition;
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.definition, newDefinition);
    }
}
