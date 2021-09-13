package org.cafienne.processtask.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

public abstract class ProcessCommand extends ModelCommand<ProcessTaskActor> {
    protected ProcessCommand(TenantUser tenantUser, String id) {
        super(tenantUser, id);
    }

    protected ProcessCommand(ValueMap json) {
        super(json);
    }

    @Override
    public final Class<ProcessTaskActor> actorClass() {
        return ProcessTaskActor.class;
    }

    @Override
    public void validate(ProcessTaskActor modelActor) throws InvalidCommandException {
        // Nothing to validate
    }
}
