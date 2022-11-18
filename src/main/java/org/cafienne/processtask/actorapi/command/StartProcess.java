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

package org.cafienne.processtask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessStarted;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class StartProcess extends ProcessCommand implements BootstrapMessage {
    private final String parentActorId;
    private final String rootActorId;
    private final String tenant;
    private final String name;
    private final ValueMap inputParameters;
    private transient ProcessDefinition definition;
    private final boolean debugMode;

    public StartProcess(UserIdentity user, String tenant, String id, String name, ProcessDefinition definition, ValueMap inputParameters, String parentActorId, String rootActorId, boolean debugMode) {
        super(user, id);
        this.name = name;
        this.tenant = tenant;
        this.parentActorId = parentActorId;
        this.rootActorId = rootActorId;
        this.inputParameters = inputParameters;
        this.definition = definition;
        this.debugMode = debugMode;
    }

    public StartProcess(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.tenant = json.readString(Fields.tenant);
        this.parentActorId = json.readString(Fields.parentActorId);
        this.rootActorId = json.readString(Fields.rootActorId);
        this.inputParameters = json.readMap(Fields.inputParameters);
        this.definition = json.readDefinition(Fields.processDefinition, ProcessDefinition.class);
        this.debugMode = json.readBoolean(Fields.debugMode);
    }

    @Override
    public String tenant() {
        return tenant;
    }

    public String getParentActorId() {
        return parentActorId;
    }

    public String getRootActorId() {
        return rootActorId;
    }

    public String getName() {
        return name;
    }

    public ValueMap getInputParameters() {
        return inputParameters;
    }

    public ProcessDefinition getDefinition() {
        return definition;
    }

    public boolean debugMode() {
        return debugMode;
    }

    @Override
    protected void process(ProcessTaskActor processTaskActor, SubProcess<?> implementation) {
        processTaskActor.addEvent(new ProcessStarted(processTaskActor, this));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.tenant, tenant);
        writeField(generator, Fields.inputParameters, inputParameters);
        writeField(generator, Fields.parentActorId, parentActorId);
        writeField(generator, Fields.rootActorId, rootActorId);
        writeField(generator, Fields.debugMode, debugMode);
        writeField(generator, Fields.processDefinition, definition);
    }
}
