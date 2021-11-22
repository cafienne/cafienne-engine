package org.cafienne.processtask.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessModified;
import org.cafienne.processtask.instance.ProcessTaskActor;

public abstract class ProcessCommand extends ModelCommand<ProcessTaskActor, UserIdentity> {
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
    public final Class<ProcessTaskActor> actorClass() {
        return ProcessTaskActor.class;
    }

    @Override
    public void validate(ProcessTaskActor modelActor) throws InvalidCommandException {
        // Nothing to validate
    }

    @Override
    public void done() {
        actor.addEvent(new ProcessModified(this, actor, actor.getTransactionTimestamp()));
    }
}
