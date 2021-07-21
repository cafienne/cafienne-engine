package org.cafienne.infrastructure.serialization;

import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.SentryEvent;
import org.cafienne.cmmn.actorapi.event.*;
import org.cafienne.cmmn.actorapi.event.file.*;
import org.cafienne.cmmn.actorapi.event.migration.CaseDefinitionMigrated;
import org.cafienne.cmmn.actorapi.event.migration.PlanItemMigrated;
import org.cafienne.cmmn.actorapi.event.plan.*;
import org.cafienne.cmmn.actorapi.event.plan.eventlistener.*;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskInputFilled;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskOutputFilled;
import org.cafienne.cmmn.actorapi.event.team.*;
import org.cafienne.humantask.actorapi.event.*;
import org.cafienne.processtask.actorapi.event.*;
import org.cafienne.tenant.actorapi.event.*;
import org.cafienne.tenant.actorapi.event.platform.TenantCreated;
import org.cafienne.tenant.actorapi.event.platform.TenantDisabled;
import org.cafienne.tenant.actorapi.event.platform.TenantEnabled;

public class EventSerializer extends CafienneSerializer {
    public static void register() {
        registerBaseEvents();
        registerCaseEvents();
        registerHumanTaskEvents();
        registerProcessEvents();
        registerTenantEvents();
        registerPlatformEvents();
    }

    private static void registerBaseEvents() {
        addManifestWrapper(DebugEvent.class, DebugEvent::new);
        addManifestWrapper(SentryEvent.class, SentryEvent::new);
        addManifestWrapper(DebugDisabled.class, DebugDisabled::new);
        addManifestWrapper(DebugEnabled.class, DebugEnabled::new);
    }

    private static void registerCaseEvents() {
        addManifestWrapper(CaseDefinitionApplied.class, CaseDefinitionApplied::new);
        addManifestWrapper(CaseModified.class, CaseModified::new);
        addManifestWrapper(CaseAppliedPlatformUpdate.class, CaseAppliedPlatformUpdate::new);
        addManifestWrapper(EngineVersionChanged.class, EngineVersionChanged::new);
        addManifestWrapper(CaseDefinitionMigrated.class, CaseDefinitionMigrated::new);
        registerCaseTeamEvents();
        registerCasePlanEvents();
        registerCaseFileEvents();
    }

    private static void registerCaseTeamEvents() {
        addManifestWrapper(TeamRoleFilled.class, TeamRoleFilled::new);
        addManifestWrapper(TeamRoleCleared.class, TeamRoleCleared::new);
        addManifestWrapper(CaseOwnerAdded.class, CaseOwnerAdded::new);
        addManifestWrapper(CaseOwnerRemoved.class, CaseOwnerRemoved::new);
        addManifestWrapper(TeamMemberAdded.class, TeamMemberAdded::new);
        addManifestWrapper(TeamMemberRemoved.class, TeamMemberRemoved::new);
    }

    private static void registerCasePlanEvents() {
        addManifestWrapper(PlanItemCreated.class, PlanItemCreated::new);
        addManifestWrapper(PlanItemTransitioned.class, PlanItemTransitioned::new);
        addManifestWrapper(PlanItemMigrated.class, PlanItemMigrated::new);
        addManifestWrapper(RepetitionRuleEvaluated.class, RepetitionRuleEvaluated::new);
        addManifestWrapper(RequiredRuleEvaluated.class, RequiredRuleEvaluated::new);
        addManifestWrapper(TaskInputFilled.class, TaskInputFilled::new);
        addManifestWrapper(TaskOutputFilled.class, TaskOutputFilled::new);
        addManifestWrapper(TimerSet.class, TimerSet::new);
        addManifestWrapper(TimerCompleted.class, TimerCompleted::new);
        addManifestWrapper(TimerTerminated.class, TimerTerminated::new);
        addManifestWrapper(TimerSuspended.class, TimerSuspended::new);
        addManifestWrapper(TimerResumed.class, TimerResumed::new);
    }

    private static void registerCaseFileEvents() {
        addManifestWrapper(CaseFileItemCreated.class, CaseFileItemCreated::new);
        addManifestWrapper(CaseFileItemUpdated.class, CaseFileItemUpdated::new);
        addManifestWrapper(CaseFileItemReplaced.class, CaseFileItemReplaced::new);
        addManifestWrapper(CaseFileItemDeleted.class, CaseFileItemDeleted::new);
        addManifestWrapper(CaseFileItemChildRemoved.class, CaseFileItemChildRemoved::new);
        // Note: CaseFileItemTransitioned event cannot be deleted, since sub class events above were introduced only in 1.1.9
        addManifestWrapper(CaseFileItemTransitioned.class, CaseFileItemTransitioned::new);
        addManifestWrapper(BusinessIdentifierSet.class, BusinessIdentifierSet::new);
        addManifestWrapper(BusinessIdentifierCleared.class, BusinessIdentifierCleared::new);
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
        addManifestWrapper(ProcessReactivated.class, ProcessReactivated::new);
        addManifestWrapper(ProcessResumed.class, ProcessResumed::new);
        addManifestWrapper(ProcessSuspended.class, ProcessSuspended::new);
        addManifestWrapper(ProcessTerminated.class, ProcessTerminated::new);
        addManifestWrapper(ProcessModified.class, ProcessModified::new);
    }

    private static void registerTenantEvents() {
        addManifestWrapper(TenantUserCreated.class, TenantUserCreated::new);
        addManifestWrapper(TenantUserUpdated.class, TenantUserUpdated::new);
        addManifestWrapper(TenantUserRoleAdded.class, TenantUserRoleAdded::new);
        addManifestWrapper(TenantUserRoleRemoved.class, TenantUserRoleRemoved::new);
        addManifestWrapper(TenantUserEnabled.class, TenantUserEnabled::new);
        addManifestWrapper(TenantUserDisabled.class, TenantUserDisabled::new);
        addManifestWrapper(OwnerAdded.class, OwnerAdded::new);
        addManifestWrapper(OwnerRemoved.class, OwnerRemoved::new);
        addManifestWrapper(TenantOwnersRequested.class, TenantOwnersRequested::new);
        addManifestWrapper(TenantModified.class, TenantModified::new);
        addManifestWrapper(TenantAppliedPlatformUpdate.class, TenantAppliedPlatformUpdate::new);
    }

    private static void registerPlatformEvents() {
        addManifestWrapper(TenantCreated.class, TenantCreated::new);
        addManifestWrapper(TenantDisabled.class, TenantDisabled::new);
        addManifestWrapper(TenantEnabled.class, TenantEnabled::new);
    }
}
