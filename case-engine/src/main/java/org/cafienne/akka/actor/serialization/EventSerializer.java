package org.cafienne.akka.actor.serialization;

import org.cafienne.akka.actor.event.EngineVersionChanged;
import org.cafienne.cmmn.akka.event.*;
import org.cafienne.cmmn.akka.event.debug.DebugDisabled;
import org.cafienne.cmmn.akka.event.debug.DebugEnabled;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.akka.event.plan.eventlistener.TimerSet;
import org.cafienne.cmmn.akka.event.plan.PlanItemCreated;
import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.akka.event.plan.RepetitionRuleEvaluated;
import org.cafienne.cmmn.akka.event.plan.RequiredRuleEvaluated;
import org.cafienne.cmmn.akka.event.plan.task.TaskInputFilled;
import org.cafienne.cmmn.akka.event.plan.task.TaskOutputFilled;
import org.cafienne.cmmn.akka.event.team.TeamMemberAdded;
import org.cafienne.cmmn.akka.event.team.TeamMemberRemoved;
import org.cafienne.humantask.akka.event.*;
import org.cafienne.processtask.akka.event.*;
import org.cafienne.tenant.akka.event.*;
import org.cafienne.tenant.akka.event.platform.TenantCreated;
import org.cafienne.tenant.akka.event.platform.TenantDisabled;
import org.cafienne.tenant.akka.event.platform.TenantEnabled;

public class EventSerializer extends AkkaCaseObjectSerializer {
    static {
        registerBaseEvents();
        registerCaseEvents();
        registerHumanTaskEvents();
        registerProcessEvents();
        registerTenantEvents();
        registerPlatformEvents();
    }

    private static void registerBaseEvents() {
        addManifestWrapper(DebugEvent.class, DebugEvent::new);
        addManifestWrapper(DebugDisabled.class, DebugDisabled::new);
        addManifestWrapper(DebugEnabled.class, DebugEnabled::new);
    }

    private static void registerCaseEvents() {
        addManifestWrapper(CaseFileEvent.class, CaseFileEvent::new);
        addManifestWrapper(CaseDefinitionApplied.class, CaseDefinitionApplied::new);
        addManifestWrapper(CaseModified.class, CaseModified::new);
        addManifestWrapper(EngineVersionChanged.class, EngineVersionChanged::new);
        addManifestWrapper(PlanItemCreated.class, PlanItemCreated::new);
        addManifestWrapper(PlanItemTransitioned.class, PlanItemTransitioned::new);
        addManifestWrapper(RepetitionRuleEvaluated.class, RepetitionRuleEvaluated::new);
        addManifestWrapper(RequiredRuleEvaluated.class, RequiredRuleEvaluated::new);
        addManifestWrapper(TaskInputFilled.class, TaskInputFilled::new);
        addManifestWrapper(TaskOutputFilled.class, TaskOutputFilled::new);
        addManifestWrapper(TeamMemberAdded.class, TeamMemberAdded::new);
        addManifestWrapper(TeamMemberRemoved.class, TeamMemberRemoved::new);
        addManifestWrapper(TimerSet.class, TimerSet::new);
    }

    private static void registerHumanTaskEvents() {
        addManifestWrapper(HumanTaskCreated.class, HumanTaskCreated::new);
        addManifestWrapper(HumanTaskActivated.class, HumanTaskActivated::new);
        addManifestWrapper(HumanTaskInputSaved.class, HumanTaskInputSaved::new);
        addManifestWrapper(HumanTaskOutputSaved.class, HumanTaskOutputSaved::new);
        addManifestWrapper(HumanTaskAssigned.class, HumanTaskAssigned::new);
        addManifestWrapper(HumanTaskClaimed.class, HumanTaskClaimed::new);
        addManifestWrapper(HumanTaskCompleted.class, HumanTaskCompleted::new);
        addManifestWrapper(HumanTaskDelegated.class, HumanTaskDelegated::new);
        addManifestWrapper(HumanTaskDueDateFilled.class, HumanTaskDueDateFilled::new);
        addManifestWrapper(HumanTaskOwnerChanged.class, HumanTaskOwnerChanged::new);
        addManifestWrapper(HumanTaskResumed.class, HumanTaskResumed::new);
        addManifestWrapper(HumanTaskRevoked.class, HumanTaskRevoked::new);
        addManifestWrapper(HumanTaskSuspended.class, HumanTaskSuspended::new);
        addManifestWrapper(HumanTaskTerminated.class, HumanTaskTerminated::new);
    }

    private static void registerProcessEvents() {
        addManifestWrapper(ProcessStarted.class, ProcessStarted::new);
        addManifestWrapper(ProcessCompleted.class, ProcessCompleted::new);
        addManifestWrapper(ProcessFailed.class, ProcessFailed::new);
        addManifestWrapper(ProcessTerminated.class, ProcessTerminated::new);
        addManifestWrapper(ProcessModified.class, ProcessModified::new);
    }

    private static void registerTenantEvents() {
        addManifestWrapper(TenantUserCreated.class, TenantUserCreated::new);
        addManifestWrapper(TenantUserRoleAdded.class, TenantUserRoleAdded::new);
        addManifestWrapper(TenantUserRoleRemoved.class, TenantUserRoleRemoved::new);
        addManifestWrapper(TenantUserEnabled.class, TenantUserEnabled::new);
        addManifestWrapper(TenantUserDisabled.class, TenantUserDisabled::new);
        addManifestWrapper(OwnerAdded.class, OwnerAdded::new);
        addManifestWrapper(OwnerRemoved.class, OwnerRemoved::new);
        addManifestWrapper(TenantOwnersRequested.class, TenantOwnersRequested::new);
        addManifestWrapper(TenantModified.class, TenantModified::new);
    }

    private static void registerPlatformEvents() {
        addManifestWrapper(TenantCreated.class, TenantCreated::new);
        addManifestWrapper(TenantDisabled.class, TenantDisabled::new);
        addManifestWrapper(TenantEnabled.class, TenantEnabled::new);
    }
}
