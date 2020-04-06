package org.cafienne.processtask.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.akka.command.response.ProcessResponse;
import org.cafienne.processtask.akka.event.ProcessReactivated;
import org.cafienne.processtask.akka.event.ProcessStarted;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ReactivateProcess extends ProcessCommand {
    private final ValueMap inputParameters;

    private enum Fields {
        name, tenant, parentActorId, rootActorId, inputParameters, processDefinition, debugMode
    }

    public ReactivateProcess(TenantUser tenantUser, String id, ValueMap inputParameters) {
        super(tenantUser, id);
        this.inputParameters = inputParameters;
    }

    public ReactivateProcess(ValueMap json) {
        super(json);
        this.inputParameters = readMap(json, Fields.inputParameters);
    }

    public ValueMap getInputParameters() {
        return inputParameters;
    }

    @Override
    public ProcessResponse process(ProcessTaskActor process) {
        return process.reactivate(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.inputParameters, inputParameters);
    }
}
