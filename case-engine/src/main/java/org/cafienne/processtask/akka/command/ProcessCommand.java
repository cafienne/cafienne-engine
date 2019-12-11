package org.cafienne.processtask.akka.command;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.processtask.instance.ProcessTaskActor;

public abstract class ProcessCommand extends ModelCommand<ProcessTaskActor> {
    protected ProcessCommand(TenantUser tenantUser, String id) {
        super(tenantUser, id);
    }

    protected ProcessCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(ProcessTaskActor modelActor) throws InvalidCommandException {
        // Nothing to validate
    }
}
