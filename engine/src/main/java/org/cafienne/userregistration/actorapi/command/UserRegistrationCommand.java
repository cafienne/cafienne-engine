package org.cafienne.userregistration.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;

public interface UserRegistrationCommand extends ModelCommand {
    default String family() {
        return this.actorId();
    }
}
