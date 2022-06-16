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

        getCaseInstance().askProcess(new StartProcess(user, tenant, taskId, taskName, task.getDefinition().getImplementationDefinition(), inputParameters, parentId, rootActorId, debugMode),
                left -> task.goFault(new ValueMap("exception", left.toJson())),
                right -> {
                    if (!task.getDefinition().isBlocking()) {
                        task.goComplete(new ValueMap());
                    }
                });
    }

    @Override
    protected void suspendInstance() {
        tell(new SuspendProcess(getCaseInstance().getCurrentUser(), task.getId()));
    }

    @Override
    protected void resumeInstance() {
        tell(new ResumeProcess(getCaseInstance().getCurrentUser(), task.getId()));
    }

    @Override
    protected void terminateInstance() {
        if (task.getHistoryState() == State.Available) {
            getCaseInstance().addDebugInfo(() -> "Terminating process task '" + task.getName() + "' without it being started; no need to inform the task actor");
        } else {
            tell(new TerminateProcess(getCaseInstance().getCurrentUser(), task.getId()));
        }
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // Apparently process has failed so we can trying again
        tell(new ReactivateProcess(getCaseInstance().getCurrentUser(), task.getId(), inputParameters));
    }

    private void tell(ProcessCommand command) {
        if (!task.getDefinition().isBlocking()) {
            return;
        }
        getCaseInstance().askProcess(command, left -> task.goFault(new ValueMap("exception", left.toJson())));
    }

    @Override
    protected void migrateDefinition(ProcessTaskDefinition newDefinition) {
        if (task.getState() != State.Null && task.getState() != State.Available) {
            task.getCaseInstance().addDebugInfo(() -> this + ": telling sub process with id "+task.getId()+" to migrate it's definition");
            tell(new MigrateProcessDefinition(getCaseInstance().getCurrentUser(), task.getId(), newDefinition.getImplementationDefinition()));
        }
    }
}
