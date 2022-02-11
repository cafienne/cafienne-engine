package org.cafienne.processtask.actorapi.command;

import org.cafienne.actormodel.command.BaseModelCommand;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.ProcessActorMessage;
import org.cafienne.processtask.instance.ProcessTaskActor;

public abstract class ProcessCommand extends BaseModelCommand<ProcessTaskActor, UserIdentity> implements ProcessActorMessage {
    protected ProcessCommand(UserIdentity user, String id) {
        super(user, id);
    }

    protected ProcessCommand(ValueMap json) {
        super(json);
    }

    @Override
    protected UserIdentity readUser(ValueMap json) {
        return UserIdentity.deserialize(json);
    }

    @Override
    public void validate(ProcessTaskActor modelActor) throws InvalidCommandException {
        // Nothing to validate
    }
}