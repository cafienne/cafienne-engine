package org.cafienne.processtask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CafienneVersion;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.akka.command.StartProcess;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ProcessStarted extends ProcessInstanceEvent {
    public final String parentActorId;
    public final String rootActorId;
    public final String name;
    public final ValueMap inputParameters;
    public transient ProcessDefinition definition;
    public final boolean debugMode;
    public final CafienneVersion engineVersion;

    private enum Fields {
        parentActorId, rootActorId, name, input, processDefinition, debugMode, engineVersion
    }

    public ProcessStarted(ProcessTaskActor actor, StartProcess command) {
        super(actor);
        this.debugMode = command.debugMode();
        this.definition = command.getDefinition();
        this.name = command.getName();
        this.parentActorId = command.getParentActorId();
        this.rootActorId = command.getRootActorId();
        this.inputParameters = command.getInputParameters();
        this.engineVersion = CaseSystem.version();
    }

    public ProcessStarted(ValueMap json) {
        super(json);
        this.engineVersion = new CafienneVersion(readMap(json, Fields.engineVersion));
        this.name = json.raw(Fields.name);
        this.parentActorId = json.raw(Fields.parentActorId);
        this.rootActorId = json.raw(Fields.rootActorId);
        this.inputParameters = readMap(json, Fields.input);
        this.definition = readDefinition(json, Fields.processDefinition, ProcessDefinition.class);
        this.debugMode = json.raw(Fields.debugMode);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }

    @Override
    public void runImmediateBehavior() {
        actor.start();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.input, inputParameters);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.parentActorId, parentActorId);
        writeField(generator, Fields.rootActorId, rootActorId);
        writeField(generator, Fields.debugMode, debugMode);
        writeField(generator, Fields.processDefinition, definition);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
