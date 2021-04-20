package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.cmmn.expression.spel.api.process.ProcessTaskAPI;
import org.cafienne.processtask.instance.ProcessTaskActor;

/**
 * Base class for giving object context to ProcessTask expressions
 * Exposes 'task' property.
 */
public abstract class ProcessActorRootObject extends APIRootObject<ProcessTaskActor> {
    private final ProcessTaskAPI context;

    protected ProcessActorRootObject(ProcessTaskActor model) {
        super(model);
        this.context = new ProcessTaskAPI(model);
        addPropertyReader("task", () -> this.context);
    }
}

