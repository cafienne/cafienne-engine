package org.cafienne.processtask.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessModified;
import org.cafienne.processtask.instance.ProcessTaskActor;

public abstract class ProcessCommand extends ModelCommand<ProcessTaskActor, TenantUser> {
    protected ProcessCommand(TenantUser user, String id) {
        super(user, id);
    }

    protected ProcessCommand(ValueMap json) {
        super(json);
    }

    @Override
    protected TenantUser readUser(ValueMap json) {
        return TenantUser.deserialize(json);
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
