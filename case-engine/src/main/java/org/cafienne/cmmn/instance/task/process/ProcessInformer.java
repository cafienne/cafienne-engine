package org.cafienne.cmmn.instance.task.process;

import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.Case;

abstract class ProcessInformer {
    static ProcessInformer getInstance(ProcessTask task, ProcessTaskDefinition definition) {
        if (definition.getImplementationDefinition().getImplementation().isInline()) {
            return new ProcessTaskInlineInformer(task);
        } else {
            return new ProcessTaskActorInformer(task);
        }
    }

    protected final ProcessTask task;

    protected ProcessInformer(ProcessTask task) {
        this.task = task;
    }

    protected Case getCaseInstance() {
        return task.getCaseInstance();
    }

    abstract protected void terminateInstance();

    abstract protected void startImplementation(ValueMap inputParameters);

    abstract protected void suspendInstance();

    abstract protected void resumeInstance();

    abstract protected void reactivateImplementation(ValueMap inputParameters);
}

