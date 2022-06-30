package org.cafienne.processtask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.MigrateProcessDefinition;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

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
