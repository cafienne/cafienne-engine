package org.cafienne.actormodel.communication;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.BaseModelCommand;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class CaseSystemCommunicationCommand extends BaseModelCommand<ModelActor, UserIdentity> implements CaseSystemCommunicationMessage {
    public final ModelCommand command;

    protected CaseSystemCommunicationCommand(ModelCommand command) {
        super(command.getUser(), command.actorId(), command.getRootCaseId());
        this.command = command;
    }

    protected CaseSystemCommunicationCommand(ValueMap json) {
        super(json);
        this.command = json.readModelCommand(Fields.command);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + "[" + command.getDescription() + "]";
    }

    @Override
    protected UserIdentity readUser(ValueMap json) {
        return UserIdentity.deserialize(json);
    }

    @Override
    public Class<?> actorClass() {
        return command.actorClass();
    }

    @Override
    public String getMessageId() {
        return command.getMessageId();
    }

    protected void writeActorCommand(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.command, command);
    }
}
