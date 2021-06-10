package org.cafienne.cmmn.instance.task.process;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.State;
import org.cafienne.processtask.actorapi.command.*;

class ProcessTaskActorInformer extends ProcessInformer {

    public ProcessTaskActorInformer(ProcessTask task) {
        super(task);
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        final String taskId = task.getId();
        final TenantUser user = getCaseInstance().getCurrentUser();
        final String taskName = task.getName();
        final String rootActorId = task.getCaseInstance().getRootActorId();
        final String parentId = task.getCaseInstance().getId();
        final boolean debugMode = task.getCaseInstance().debugMode();

        getCaseInstance().askProcess(new StartProcess(user, taskId, taskName, task.getDefinition().getImplementationDefinition(), inputParameters, parentId, rootActorId, debugMode),
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
    }}
