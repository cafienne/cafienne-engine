package org.cafienne.engine.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;

public interface CaseEngineCommand extends ModelCommand {
    default String family() {
        return this.actorId();
    }
}
