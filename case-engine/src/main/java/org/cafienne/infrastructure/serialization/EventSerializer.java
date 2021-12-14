package org.cafienne.infrastructure.serialization;

import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.SentryEvent;
import org.cafienne.cmmn.actorapi.event.*;
import org.cafienne.cmmn.actorapi.event.file.*;
import org.cafienne.cmmn.actorapi.event.migration.*;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.actorapi.event.plan.RepetitionRuleEvaluated;
import org.cafienne.cmmn.actorapi.event.plan.RequiredRuleEvaluated;
import org.cafienne.cmmn.actorapi.event.plan.eventlistener.*;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskInputFilled;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskOutputFilled;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.CaseOwnerAdded;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.CaseOwnerRemoved;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.TeamRoleCleared;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.TeamRoleFilled;
import org.cafienne.cmmn.actorapi.event.team.deprecated.user.TeamMemberAdded;
import org.cafienne.cmmn.actorapi.event.team.deprecated.user.TeamMemberRemoved;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleRemoved;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserRemoved;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupAdded;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupChanged;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupRemoved;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleAdded;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleChanged;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserAdded;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserChanged;
import org.cafienne.consentgroup.actorapi.event.*;
import org.cafienne.humantask.actorapi.event.*;
import org.cafienne.humantask.actorapi.event.migration.HumanTaskDropped;
import org.cafienne.humantask.actorapi.event.migration.HumanTaskMigrated;
import org.cafienne.processtask.actorapi.event.*;
import org.cafienne.tenant.actorapi.event.*;
import org.cafienne.tenant.actorapi.event.deprecated.*;
import org.cafienne.tenant.actorapi.event.platform.TenantCreated;
import org.cafienne.tenant.actorapi.event.platform.TenantDisabled;
import org.cafienne.tenant.actorapi.event.platform.TenantEnabled;
import org.cafienne.tenant.actorapi.event.user.TenantUserAdded;
import org.cafienne.tenant.actorapi.event.user.TenantUserChanged;
import org.cafienne.tenant.actorapi.event.user.TenantUserRemoved;

public class EventSerializer extends CafienneSerializer {
    public static void register() {
        registerBaseEvents();
        registerCaseEvents();
        registerHumanTaskEvents();
        registerProcessEvents();
        registerTenantEvents();
        registerConsentGroupEvents();
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
        registerCaseTeamMemberEvents();
        registerDeprecatedCaseTeamEvents();
    }

    private static void registerCaseTeamMemberEvents() {
        addManifestWrapper(CaseTeamUserAdded.class, CaseTeamUserAdded::new);
        addManifestWrapper(CaseTeamUserChanged.class, CaseTeamUserChanged::new);
        addManifestWrapper(CaseTeamUserRemoved.class, CaseTeamUserRemoved::new);
        addManifestWrapper(CaseTeamGroupAdded.class, CaseTeamGroupAdded::new);
        addManifestWrapper(CaseTeamGroupChanged.class, CaseTeamGroupChanged::new);
        addManifestWrapper(CaseTeamGroupRemoved.class, CaseTeamGroupRemoved::new);
        addManifestWrapper(CaseTeamTenantRoleAdded.class, CaseTeamTenantRoleAdded::new);
        addManifestWrapper(CaseTeamTenantRoleChanged.class, CaseTeamTenantRoleChanged::new);
        addManifestWrapper(CaseTeamTenantRoleRemoved.class, CaseTeamTenantRoleRemoved::new);
    }

    private static void registerDeprecatedCaseTeamEvents() {
        // The newest old ones
        addManifestWrapper(TeamRoleFilled.class, TeamRoleFilled::new);
        addManifestWrapper(TeamRoleCleared.class, TeamRoleCleared::new);
        addManifestWrapper(CaseOwnerAdded.class, CaseOwnerAdded::new);
        addManifestWrapper(CaseOwnerRemoved.class, CaseOwnerRemoved::new);
        // Even older ones
        addManifestWrapper(TeamMemberAdded.class, TeamMemberAdded::new);
        addManifestWrapper(TeamMemberRemoved.class, TeamMemberRemoved::new);
    }

    private static void registerCasePlanEvents() {
        addManifestWrapper(PlanItemCreated.class, PlanItemCreated::new);
        addManifestWrapper(PlanItemTransitioned.class, PlanItemTransitioned::new);
        addManifestWrapper(PlanItemMigrated.class, PlanItemMigrated::new);
        addManifestWrapper(PlanItemDropped.class, PlanItemDropped::new);
        addManifestWrapper(RepetitionRuleEvaluated.class, RepetitionRuleEvaluated::new);
        addManifestWrapper(RequiredRuleEvaluated.class, RequiredRuleEvaluated::new);
        addManifestWrapper(TaskInputFilled.class, TaskInputFilled::new);
        addManifestWrapper(TaskOutputFilled.class, TaskOutputFilled::new);
        addManifestWrapper(TimerSet.class, TimerSet::new);
        addManifestWrapper(TimerCompleted.class, TimerCompleted::new);
        addManifestWrapper(TimerTerminated.class, TimerTerminated::new);
        addManifestWrapper(TimerSuspended.class, TimerSuspended::new);
        addManifestWrapper(TimerResumed.class, TimerResumed::new);
        addManifestWrapper(TimerDropped.class, TimerDropped::new);
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
        addManifestWrapper(CaseFileItemMigrated.class, CaseFileItemMigrated::new);
        addManifestWrapper(CaseFileItemDropped.class, CaseFileItemDropped::new);
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
        addManifestWrapper(HumanTaskMigrated.class, HumanTaskMigrated::new);
        addManifestWrapper(HumanTaskDropped.class, HumanTaskDropped::new);
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
        addManifestWrapper(TenantOwnersRequested.class, TenantOwnersRequested::new);
        addManifestWrapper(TenantModified.class, TenantModified::new);
        addManifestWrapper(TenantAppliedPlatformUpdate.class, TenantAppliedPlatformUpdate::new);
        addManifestWrapper(TenantUserAdded.class, TenantUserAdded::new);
        addManifestWrapper(TenantUserChanged.class, TenantUserChanged::new);
        addManifestWrapper(TenantUserRemoved.class, TenantUserRemoved::new);
        registerDeprecatedTenantEvents();
    }

    private static void registerDeprecatedTenantEvents() {
        addManifestWrapper(TenantUserCreated.class, TenantUserCreated::new);
        addManifestWrapper(TenantUserUpdated.class, TenantUserUpdated::new);
        addManifestWrapper(TenantUserRoleAdded.class, TenantUserRoleAdded::new);
        addManifestWrapper(TenantUserRoleRemoved.class, TenantUserRoleRemoved::new);
        addManifestWrapper(TenantUserEnabled.class, TenantUserEnabled::new);
        addManifestWrapper(TenantUserDisabled.class, TenantUserDisabled::new);
        addManifestWrapper(OwnerAdded.class, OwnerAdded::new);
        addManifestWrapper(OwnerRemoved.class, OwnerRemoved::new);
    }

    private static void registerConsentGroupEvents() {
        CafienneSerializer.addManifestWrapper(ConsentGroupMemberAdded.class, ConsentGroupMemberAdded::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupMemberChanged.class, ConsentGroupMemberChanged::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupMemberRemoved.class, ConsentGroupMemberRemoved::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupCreated.class, ConsentGroupCreated::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupModified.class, ConsentGroupModified::new);
    }

    private static void registerPlatformEvents() {
        addManifestWrapper(TenantCreated.class, TenantCreated::new);
        addManifestWrapper(TenantDisabled.class, TenantDisabled::new);
        addManifestWrapper(TenantEnabled.class, TenantEnabled::new);
    }
}
