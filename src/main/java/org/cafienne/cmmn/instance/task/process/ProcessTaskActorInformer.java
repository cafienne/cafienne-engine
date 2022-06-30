package org.cafienne.cmmn.instance.task.process;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.*;

class ProcessTaskActorInformer extends ProcessInformer {

    public ProcessTaskActorInformer(ProcessTask task) {
        super(task);
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        final String taskId = task.getId();
        final CaseUserIdentity user = getCaseInstance().getCurrentUser();
        final String tenant = getCaseInstance().getTenant();
        final String taskName = task.getName();
        final String rootActorId = task.getCaseInstance().getRootActorId();
        final String parentId = task.getCaseInstance().getId();
        final boolean debugMode = task.getCaseInstance().debugMode();
        final StartProcess command = new StartProcess(user, tenant, taskId, taskName, task.getDefinition().getImplementationDefinition(), inputParameters, parentId, rootActorId, debugMode);

        task.startTaskImplementation(command);
    }

    @Override
    protected void suspendInstance() {
        task.tellTaskImplementation(new SuspendProcess(getCaseInstance().getCurrentUser(), task.getId()));
    }

    @Override
    protected void resumeInstance() {
        task.tellTaskImplementation(new ResumeProcess(getCaseInstance().getCurrentUser(), task.getId()));
    }

    @Override
    protected void terminateInstance() {
        if (task.getHistoryState() == State.Available) {
            getCaseInstance().addDebugInfo(() -> "Terminating process task '" + task.getName() + "' without it being started; no need to inform the task actor");
        } else {
            task.tellTaskImplementation(new TerminateProcess(getCaseInstance().getCurrentUser(), task.getId()));
        }
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // TODO: reactivate is invoked after actual state has become active again,
        //  and that means that task.getImplementationState.getStarted returns the wrong value
        // NOT SURE WHAT THE IMPACT OF SUCH A SCENARIO is
        if (task.getImplementationState().isStarted()) {
            // Apparently process has failed so we can trying again
            task.tellTaskImplementation(new ReactivateProcess(getCaseInstance().getCurrentUser(), task.getId(), inputParameters));
        } else {
            startImplementation(inputParameters);
        }
    }

    @Override
    protected void migrateDefinition(ProcessTaskDefinition newDefinition) {
        task.giveNewDefinition(new MigrateProcessDefinition(getCaseInstance().getCurrentUser(), task.getId(), newDefinition.getImplementationDefinition()));
    }
}
