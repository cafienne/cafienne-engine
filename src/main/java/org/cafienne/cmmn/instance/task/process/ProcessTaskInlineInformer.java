package org.cafienne.cmmn.instance.task.process;

import org.cafienne.json.ValueMap;
import org.cafienne.processtask.implementation.InlineSubProcess;

class ProcessTaskInlineInformer extends ProcessInformer {

    private final InlineSubProcess implementation;

    public ProcessTaskInlineInformer(ProcessTask task) {
        super(task);
        implementation = task.getDefinition().getImplementationDefinition().getInlineImplementation().createInstance(task);
    }

    @Override
    protected void terminateInstance() {
        implementation.terminate();
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        implementation.start();
    }

    @Override
    protected void suspendInstance() {
        implementation.suspend();
    }

    @Override
    protected void resumeInstance() {
        implementation.resume();
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        implementation.reactivate();
    }
}
