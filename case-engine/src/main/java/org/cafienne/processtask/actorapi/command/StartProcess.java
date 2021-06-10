package org.cafienne.processtask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapCommand;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class StartProcess extends ProcessCommand implements BootstrapCommand {
    private final String parentActorId;
    private final String rootActorId;
    private final String tenant;
    private final String name;
    private final ValueMap inputParameters;
    private transient ProcessDefinition definition;
    private final boolean debugMode;

    public StartProcess(TenantUser tenantUser, String id, String name, ProcessDefinition definition, ValueMap inputParameters, String parentActorId, String rootActorId, boolean debugMode) {
        super(tenantUser, id);
        this.name = name;
        this.tenant = tenantUser.tenant();
        this.parentActorId = parentActorId;
        this.rootActorId = rootActorId;
        this.inputParameters = inputParameters;
        this.definition = definition;
        this.debugMode = debugMode;
    }

    public StartProcess(ValueMap json) {
        super(json);
        this.name = json.raw(Fields.name);
        this.tenant = readField(json, Fields.tenant);
        this.parentActorId = json.raw(Fields.parentActorId);
        this.rootActorId = json.raw(Fields.rootActorId);
        this.inputParameters = readMap(json, Fields.inputParameters);
        this.definition = readDefinition(json, Fields.processDefinition, ProcessDefinition.class);
        this.debugMode = json.raw(Fields.debugMode);
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
    public ProcessResponse process(ProcessTaskActor process) {
        return process.start(this);
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
