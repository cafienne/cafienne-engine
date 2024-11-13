/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.infrastructure.serialization.serializers;

import com.casefabric.actormodel.event.DebugEvent;
import com.casefabric.actormodel.event.EngineVersionChanged;
import com.casefabric.actormodel.event.SentryEvent;
import com.casefabric.cmmn.actorapi.event.*;
import com.casefabric.cmmn.actorapi.event.file.*;
import com.casefabric.cmmn.actorapi.event.migration.*;
import com.casefabric.cmmn.actorapi.event.plan.PlanItemCreated;
import com.casefabric.cmmn.actorapi.event.plan.PlanItemTransitioned;
import com.casefabric.cmmn.actorapi.event.plan.RepetitionRuleEvaluated;
import com.casefabric.cmmn.actorapi.event.plan.RequiredRuleEvaluated;
import com.casefabric.cmmn.actorapi.event.plan.eventlistener.*;
import com.casefabric.cmmn.actorapi.event.plan.task.*;
import com.casefabric.cmmn.actorapi.event.team.deprecated.member.CaseOwnerAdded;
import com.casefabric.cmmn.actorapi.event.team.deprecated.member.CaseOwnerRemoved;
import com.casefabric.cmmn.actorapi.event.team.deprecated.member.TeamRoleCleared;
import com.casefabric.cmmn.actorapi.event.team.deprecated.member.TeamRoleFilled;
import com.casefabric.cmmn.actorapi.event.team.deprecated.user.TeamMemberAdded;
import com.casefabric.cmmn.actorapi.event.team.deprecated.user.TeamMemberRemoved;
import com.casefabric.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleRemoved;
import com.casefabric.cmmn.actorapi.event.team.user.CaseTeamUserRemoved;
import com.casefabric.cmmn.actorapi.event.team.group.CaseTeamGroupAdded;
import com.casefabric.cmmn.actorapi.event.team.group.CaseTeamGroupChanged;
import com.casefabric.cmmn.actorapi.event.team.group.CaseTeamGroupRemoved;
import com.casefabric.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleAdded;
import com.casefabric.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleChanged;
import com.casefabric.cmmn.actorapi.event.team.user.CaseTeamUserAdded;
import com.casefabric.cmmn.actorapi.event.team.user.CaseTeamUserChanged;
import com.casefabric.consentgroup.actorapi.event.*;
import com.casefabric.humantask.actorapi.event.*;
import com.casefabric.humantask.actorapi.event.migration.HumanTaskDropped;
import com.casefabric.humantask.actorapi.event.migration.HumanTaskMigrated;
import com.casefabric.infrastructure.serialization.CaseFabricSerializer;
import com.casefabric.processtask.actorapi.event.*;
import com.casefabric.tenant.actorapi.event.*;
import com.casefabric.tenant.actorapi.event.deprecated.*;
import com.casefabric.tenant.actorapi.event.platform.TenantCreated;
import com.casefabric.tenant.actorapi.event.platform.TenantDisabled;
import com.casefabric.tenant.actorapi.event.platform.TenantEnabled;
import com.casefabric.tenant.actorapi.event.user.TenantUserAdded;
import com.casefabric.tenant.actorapi.event.user.TenantUserChanged;
import com.casefabric.tenant.actorapi.event.user.TenantUserRemoved;

public class EventSerializers {
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
        CaseFabricSerializer.addManifestWrapper(DebugEvent.class, DebugEvent::new);
        CaseFabricSerializer.addManifestWrapper(SentryEvent.class, SentryEvent::new);
        CaseFabricSerializer.addManifestWrapper(DebugDisabled.class, DebugDisabled::new);
        CaseFabricSerializer.addManifestWrapper(DebugEnabled.class, DebugEnabled::new);
    }

    private static void registerCaseEvents() {
        CaseFabricSerializer.addManifestWrapper(CaseDefinitionApplied.class, CaseDefinitionApplied::new);
        CaseFabricSerializer.addManifestWrapper(CaseModified.class, CaseModified::new);
        CaseFabricSerializer.addManifestWrapper(CaseAppliedPlatformUpdate.class, CaseAppliedPlatformUpdate::new);
        CaseFabricSerializer.addManifestWrapper(EngineVersionChanged.class, EngineVersionChanged::new);
        CaseFabricSerializer.addManifestWrapper(CaseDefinitionMigrated.class, CaseDefinitionMigrated::new);
        CaseFabricSerializer.addManifestWrapper(CaseOutputFilled.class, CaseOutputFilled::new);
        registerCaseTeamEvents();
        registerCasePlanEvents();
        registerCaseFileEvents();
    }

    private static void registerCaseTeamEvents() {
        registerCaseTeamMemberEvents();
        registerDeprecatedCaseTeamEvents();
    }

    private static void registerCaseTeamMemberEvents() {
        CaseFabricSerializer.addManifestWrapper(CaseTeamUserAdded.class, CaseTeamUserAdded::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamUserChanged.class, CaseTeamUserChanged::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamUserRemoved.class, CaseTeamUserRemoved::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamGroupAdded.class, CaseTeamGroupAdded::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamGroupChanged.class, CaseTeamGroupChanged::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamGroupRemoved.class, CaseTeamGroupRemoved::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamTenantRoleAdded.class, CaseTeamTenantRoleAdded::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamTenantRoleChanged.class, CaseTeamTenantRoleChanged::new);
        CaseFabricSerializer.addManifestWrapper(CaseTeamTenantRoleRemoved.class, CaseTeamTenantRoleRemoved::new);
    }

    private static void registerDeprecatedCaseTeamEvents() {
        // The newest old ones
        CaseFabricSerializer.addManifestWrapper(TeamRoleFilled.class, TeamRoleFilled::new);
        CaseFabricSerializer.addManifestWrapper(TeamRoleCleared.class, TeamRoleCleared::new);
        CaseFabricSerializer.addManifestWrapper(CaseOwnerAdded.class, CaseOwnerAdded::new);
        CaseFabricSerializer.addManifestWrapper(CaseOwnerRemoved.class, CaseOwnerRemoved::new);
        // Even older ones
        CaseFabricSerializer.addManifestWrapper(TeamMemberAdded.class, TeamMemberAdded::new);
        CaseFabricSerializer.addManifestWrapper(TeamMemberRemoved.class, TeamMemberRemoved::new);
    }

    private static void registerCasePlanEvents() {
        CaseFabricSerializer.addManifestWrapper(PlanItemCreated.class, PlanItemCreated::new);
        CaseFabricSerializer.addManifestWrapper(PlanItemTransitioned.class, PlanItemTransitioned::new);
        CaseFabricSerializer.addManifestWrapper(PlanItemMigrated.class, PlanItemMigrated::new);
        CaseFabricSerializer.addManifestWrapper(PlanItemDropped.class, PlanItemDropped::new);
        CaseFabricSerializer.addManifestWrapper(RepetitionRuleEvaluated.class, RepetitionRuleEvaluated::new);
        CaseFabricSerializer.addManifestWrapper(RequiredRuleEvaluated.class, RequiredRuleEvaluated::new);
        CaseFabricSerializer.addManifestWrapper(TaskInputFilled.class, TaskInputFilled::new);
        CaseFabricSerializer.addManifestWrapper(TaskOutputFilled.class, TaskOutputFilled::new);
        CaseFabricSerializer.addManifestWrapper(TaskImplementationStarted.class, TaskImplementationStarted::new);
        CaseFabricSerializer.addManifestWrapper(TaskCommandRejected.class, TaskCommandRejected::new);
        CaseFabricSerializer.addManifestWrapper(TaskImplementationNotStarted.class, TaskImplementationNotStarted::new);
        CaseFabricSerializer.addManifestWrapper(TaskImplementationReactivated.class, TaskImplementationReactivated::new);
        CaseFabricSerializer.addManifestWrapper(TimerSet.class, TimerSet::new);
        CaseFabricSerializer.addManifestWrapper(TimerCompleted.class, TimerCompleted::new);
        CaseFabricSerializer.addManifestWrapper(TimerTerminated.class, TimerTerminated::new);
        CaseFabricSerializer.addManifestWrapper(TimerSuspended.class, TimerSuspended::new);
        CaseFabricSerializer.addManifestWrapper(TimerResumed.class, TimerResumed::new);
        CaseFabricSerializer.addManifestWrapper(TimerDropped.class, TimerDropped::new);
    }

    private static void registerCaseFileEvents() {
        CaseFabricSerializer.addManifestWrapper(CaseFileItemCreated.class, CaseFileItemCreated::new);
        CaseFabricSerializer.addManifestWrapper(CaseFileItemUpdated.class, CaseFileItemUpdated::new);
        CaseFabricSerializer.addManifestWrapper(CaseFileItemReplaced.class, CaseFileItemReplaced::new);
        CaseFabricSerializer.addManifestWrapper(CaseFileItemDeleted.class, CaseFileItemDeleted::new);
        CaseFabricSerializer.addManifestWrapper(CaseFileItemChildRemoved.class, CaseFileItemChildRemoved::new);
        // Note: CaseFileItemTransitioned event cannot be deleted, since sub class events above were introduced only in 1.1.9
        CaseFabricSerializer.addManifestWrapper(CaseFileItemTransitioned.class, CaseFileItemTransitioned::new);
        CaseFabricSerializer.addManifestWrapper(BusinessIdentifierSet.class, BusinessIdentifierSet::new);
        CaseFabricSerializer.addManifestWrapper(BusinessIdentifierCleared.class, BusinessIdentifierCleared::new);
        CaseFabricSerializer.addManifestWrapper(CaseFileItemMigrated.class, CaseFileItemMigrated::new);
        CaseFabricSerializer.addManifestWrapper(CaseFileItemDropped.class, CaseFileItemDropped::new);
    }

    private static void registerHumanTaskEvents() {
        CaseFabricSerializer.addManifestWrapper(HumanTaskCreated.class, HumanTaskCreated::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskActivated.class, HumanTaskActivated::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskInputSaved.class, HumanTaskInputSaved::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskOutputSaved.class, HumanTaskOutputSaved::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskAssigned.class, HumanTaskAssigned::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskClaimed.class, HumanTaskClaimed::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskCompleted.class, HumanTaskCompleted::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskDelegated.class, HumanTaskDelegated::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskDueDateFilled.class, HumanTaskDueDateFilled::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskOwnerChanged.class, HumanTaskOwnerChanged::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskResumed.class, HumanTaskResumed::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskRevoked.class, HumanTaskRevoked::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskSuspended.class, HumanTaskSuspended::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskTerminated.class, HumanTaskTerminated::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskMigrated.class, HumanTaskMigrated::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskDropped.class, HumanTaskDropped::new);
    }

    private static void registerProcessEvents() {
        CaseFabricSerializer.addManifestWrapper(ProcessStarted.class, ProcessStarted::new);
        CaseFabricSerializer.addManifestWrapper(ProcessCompleted.class, ProcessCompleted::new);
        CaseFabricSerializer.addManifestWrapper(ProcessFailed.class, ProcessFailed::new);
        CaseFabricSerializer.addManifestWrapper(ProcessReactivated.class, ProcessReactivated::new);
        CaseFabricSerializer.addManifestWrapper(ProcessResumed.class, ProcessResumed::new);
        CaseFabricSerializer.addManifestWrapper(ProcessSuspended.class, ProcessSuspended::new);
        CaseFabricSerializer.addManifestWrapper(ProcessTerminated.class, ProcessTerminated::new);
        CaseFabricSerializer.addManifestWrapper(ProcessModified.class, ProcessModified::new);
        CaseFabricSerializer.addManifestWrapper(ProcessDefinitionMigrated.class, ProcessDefinitionMigrated::new);
    }

    private static void registerTenantEvents() {
        CaseFabricSerializer.addManifestWrapper(TenantOwnersRequested.class, TenantOwnersRequested::new);
        CaseFabricSerializer.addManifestWrapper(TenantModified.class, TenantModified::new);
        CaseFabricSerializer.addManifestWrapper(TenantAppliedPlatformUpdate.class, TenantAppliedPlatformUpdate::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserAdded.class, TenantUserAdded::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserChanged.class, TenantUserChanged::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserRemoved.class, TenantUserRemoved::new);
        registerDeprecatedTenantEvents();
    }

    private static void registerDeprecatedTenantEvents() {
        CaseFabricSerializer.addManifestWrapper(TenantUserCreated.class, TenantUserCreated::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserUpdated.class, TenantUserUpdated::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserRoleAdded.class, TenantUserRoleAdded::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserRoleRemoved.class, TenantUserRoleRemoved::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserEnabled.class, TenantUserEnabled::new);
        CaseFabricSerializer.addManifestWrapper(TenantUserDisabled.class, TenantUserDisabled::new);
        CaseFabricSerializer.addManifestWrapper(OwnerAdded.class, OwnerAdded::new);
        CaseFabricSerializer.addManifestWrapper(OwnerRemoved.class, OwnerRemoved::new);
    }

    private static void registerConsentGroupEvents() {
        CaseFabricSerializer.addManifestWrapper(ConsentGroupMemberAdded.class, ConsentGroupMemberAdded::new);
        CaseFabricSerializer.addManifestWrapper(ConsentGroupMemberChanged.class, ConsentGroupMemberChanged::new);
        CaseFabricSerializer.addManifestWrapper(ConsentGroupMemberRemoved.class, ConsentGroupMemberRemoved::new);
        CaseFabricSerializer.addManifestWrapper(ConsentGroupCreated.class, ConsentGroupCreated::new);
        CaseFabricSerializer.addManifestWrapper(ConsentGroupModified.class, ConsentGroupModified::new);
    }

    private static void registerPlatformEvents() {
        CaseFabricSerializer.addManifestWrapper(TenantCreated.class, TenantCreated::new);
        CaseFabricSerializer.addManifestWrapper(TenantDisabled.class, TenantDisabled::new);
        CaseFabricSerializer.addManifestWrapper(TenantEnabled.class, TenantEnabled::new);
    }
}
