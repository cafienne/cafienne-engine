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

package org.cafienne.infrastructure.serialization.serializers;

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
import org.cafienne.cmmn.actorapi.event.plan.task.*;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.CaseOwnerAdded;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.CaseOwnerRemoved;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.TeamRoleCleared;
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.TeamRoleFilled;
import org.cafienne.cmmn.actorapi.event.team.deprecated.user.TeamMemberAdded;
import org.cafienne.cmmn.actorapi.event.team.deprecated.user.TeamMemberRemoved;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupAdded;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupChanged;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupRemoved;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleAdded;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleChanged;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleRemoved;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserAdded;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserChanged;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserRemoved;
import org.cafienne.consentgroup.actorapi.event.*;
import org.cafienne.humantask.actorapi.event.*;
import org.cafienne.humantask.actorapi.event.migration.HumanTaskDropped;
import org.cafienne.humantask.actorapi.event.migration.HumanTaskMigrated;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.processtask.actorapi.event.*;
import org.cafienne.tenant.actorapi.event.TenantAppliedPlatformUpdate;
import org.cafienne.tenant.actorapi.event.TenantModified;
import org.cafienne.tenant.actorapi.event.deprecated.*;
import org.cafienne.tenant.actorapi.event.platform.TenantCreated;
import org.cafienne.tenant.actorapi.event.platform.TenantDisabled;
import org.cafienne.tenant.actorapi.event.platform.TenantEnabled;
import org.cafienne.tenant.actorapi.event.user.TenantUserAdded;
import org.cafienne.tenant.actorapi.event.user.TenantUserChanged;
import org.cafienne.tenant.actorapi.event.user.TenantUserRemoved;

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
        CafienneSerializer.addManifestWrapper(DebugEvent.class, DebugEvent::new);
        CafienneSerializer.addManifestWrapper(SentryEvent.class, SentryEvent::new);
        CafienneSerializer.addManifestWrapper(DebugDisabled.class, DebugDisabled::new);
        CafienneSerializer.addManifestWrapper(DebugEnabled.class, DebugEnabled::new);
    }

    private static void registerCaseEvents() {
        CafienneSerializer.addManifestWrapper(CaseDefinitionApplied.class, CaseDefinitionApplied::new);
        CafienneSerializer.addManifestWrapper(CaseModified.class, CaseModified::new);
        CafienneSerializer.addManifestWrapper(CaseAppliedPlatformUpdate.class, CaseAppliedPlatformUpdate::new);
        CafienneSerializer.addManifestWrapper(EngineVersionChanged.class, EngineVersionChanged::new);
        CafienneSerializer.addManifestWrapper(CaseDefinitionMigrated.class, CaseDefinitionMigrated::new);
        CafienneSerializer.addManifestWrapper(CaseOutputFilled.class, CaseOutputFilled::new);
        registerCaseTeamEvents();
        registerCasePlanEvents();
        registerCaseFileEvents();
    }

    private static void registerCaseTeamEvents() {
        registerCaseTeamMemberEvents();
        registerDeprecatedCaseTeamEvents();
    }

    private static void registerCaseTeamMemberEvents() {
        CafienneSerializer.addManifestWrapper(CaseTeamUserAdded.class, CaseTeamUserAdded::new);
        CafienneSerializer.addManifestWrapper(CaseTeamUserChanged.class, CaseTeamUserChanged::new);
        CafienneSerializer.addManifestWrapper(CaseTeamUserRemoved.class, CaseTeamUserRemoved::new);
        CafienneSerializer.addManifestWrapper(CaseTeamGroupAdded.class, CaseTeamGroupAdded::new);
        CafienneSerializer.addManifestWrapper(CaseTeamGroupChanged.class, CaseTeamGroupChanged::new);
        CafienneSerializer.addManifestWrapper(CaseTeamGroupRemoved.class, CaseTeamGroupRemoved::new);
        CafienneSerializer.addManifestWrapper(CaseTeamTenantRoleAdded.class, CaseTeamTenantRoleAdded::new);
        CafienneSerializer.addManifestWrapper(CaseTeamTenantRoleChanged.class, CaseTeamTenantRoleChanged::new);
        CafienneSerializer.addManifestWrapper(CaseTeamTenantRoleRemoved.class, CaseTeamTenantRoleRemoved::new);
    }

    private static void registerDeprecatedCaseTeamEvents() {
        // The newest old ones
        CafienneSerializer.addManifestWrapper(TeamRoleFilled.class, TeamRoleFilled::new);
        CafienneSerializer.addManifestWrapper(TeamRoleCleared.class, TeamRoleCleared::new);
        CafienneSerializer.addManifestWrapper(CaseOwnerAdded.class, CaseOwnerAdded::new);
        CafienneSerializer.addManifestWrapper(CaseOwnerRemoved.class, CaseOwnerRemoved::new);
        // Even older ones
        CafienneSerializer.addManifestWrapper(TeamMemberAdded.class, TeamMemberAdded::new);
        CafienneSerializer.addManifestWrapper(TeamMemberRemoved.class, TeamMemberRemoved::new);
    }

    private static void registerCasePlanEvents() {
        CafienneSerializer.addManifestWrapper(PlanItemCreated.class, PlanItemCreated::new);
        CafienneSerializer.addManifestWrapper(PlanItemTransitioned.class, PlanItemTransitioned::new);
        CafienneSerializer.addManifestWrapper(PlanItemMigrated.class, PlanItemMigrated::new);
        CafienneSerializer.addManifestWrapper(PlanItemDropped.class, PlanItemDropped::new);
        CafienneSerializer.addManifestWrapper(RepetitionRuleEvaluated.class, RepetitionRuleEvaluated::new);
        CafienneSerializer.addManifestWrapper(RequiredRuleEvaluated.class, RequiredRuleEvaluated::new);
        CafienneSerializer.addManifestWrapper(TaskInputFilled.class, TaskInputFilled::new);
        CafienneSerializer.addManifestWrapper(TaskOutputFilled.class, TaskOutputFilled::new);
        CafienneSerializer.addManifestWrapper(TaskImplementationStarted.class, TaskImplementationStarted::new);
        CafienneSerializer.addManifestWrapper(TaskCommandRejected.class, TaskCommandRejected::new);
        CafienneSerializer.addManifestWrapper(TaskImplementationNotStarted.class, TaskImplementationNotStarted::new);
        CafienneSerializer.addManifestWrapper(TaskImplementationReactivated.class, TaskImplementationReactivated::new);
        CafienneSerializer.addManifestWrapper(TimerSet.class, TimerSet::new);
        CafienneSerializer.addManifestWrapper(TimerCompleted.class, TimerCompleted::new);
        CafienneSerializer.addManifestWrapper(TimerTerminated.class, TimerTerminated::new);
        CafienneSerializer.addManifestWrapper(TimerSuspended.class, TimerSuspended::new);
        CafienneSerializer.addManifestWrapper(TimerResumed.class, TimerResumed::new);
        CafienneSerializer.addManifestWrapper(TimerDropped.class, TimerDropped::new);
    }

    private static void registerCaseFileEvents() {
        CafienneSerializer.addManifestWrapper(CaseFileItemCreated.class, CaseFileItemCreated::new);
        CafienneSerializer.addManifestWrapper(CaseFileItemUpdated.class, CaseFileItemUpdated::new);
        CafienneSerializer.addManifestWrapper(CaseFileItemReplaced.class, CaseFileItemReplaced::new);
        CafienneSerializer.addManifestWrapper(CaseFileItemDeleted.class, CaseFileItemDeleted::new);
        CafienneSerializer.addManifestWrapper(CaseFileItemChildRemoved.class, CaseFileItemChildRemoved::new);
        // Note: CaseFileItemTransitioned event cannot be deleted, since sub class events above were introduced only in 1.1.9
        CafienneSerializer.addManifestWrapper(CaseFileItemTransitioned.class, CaseFileItemTransitioned::new);
        CafienneSerializer.addManifestWrapper(BusinessIdentifierSet.class, BusinessIdentifierSet::new);
        CafienneSerializer.addManifestWrapper(BusinessIdentifierCleared.class, BusinessIdentifierCleared::new);
        CafienneSerializer.addManifestWrapper(CaseFileItemMigrated.class, CaseFileItemMigrated::new);
        CafienneSerializer.addManifestWrapper(CaseFileItemDropped.class, CaseFileItemDropped::new);
    }

    private static void registerHumanTaskEvents() {
        CafienneSerializer.addManifestWrapper(HumanTaskCreated.class, HumanTaskCreated::new);
        CafienneSerializer.addManifestWrapper(HumanTaskActivated.class, HumanTaskActivated::new);
        CafienneSerializer.addManifestWrapper(HumanTaskInputSaved.class, HumanTaskInputSaved::new);
        CafienneSerializer.addManifestWrapper(HumanTaskOutputSaved.class, HumanTaskOutputSaved::new);
        CafienneSerializer.addManifestWrapper(HumanTaskAssigned.class, HumanTaskAssigned::new);
        CafienneSerializer.addManifestWrapper(HumanTaskClaimed.class, HumanTaskClaimed::new);
        CafienneSerializer.addManifestWrapper(HumanTaskCompleted.class, HumanTaskCompleted::new);
        CafienneSerializer.addManifestWrapper(HumanTaskDelegated.class, HumanTaskDelegated::new);
        CafienneSerializer.addManifestWrapper(HumanTaskDueDateFilled.class, HumanTaskDueDateFilled::new);
        CafienneSerializer.addManifestWrapper(HumanTaskOwnerChanged.class, HumanTaskOwnerChanged::new);
        CafienneSerializer.addManifestWrapper(HumanTaskResumed.class, HumanTaskResumed::new);
        CafienneSerializer.addManifestWrapper(HumanTaskRevoked.class, HumanTaskRevoked::new);
        CafienneSerializer.addManifestWrapper(HumanTaskSuspended.class, HumanTaskSuspended::new);
        CafienneSerializer.addManifestWrapper(HumanTaskTerminated.class, HumanTaskTerminated::new);
        CafienneSerializer.addManifestWrapper(HumanTaskMigrated.class, HumanTaskMigrated::new);
        CafienneSerializer.addManifestWrapper(HumanTaskDropped.class, HumanTaskDropped::new);
    }

    private static void registerProcessEvents() {
        CafienneSerializer.addManifestWrapper(ProcessStarted.class, ProcessStarted::new);
        CafienneSerializer.addManifestWrapper(ProcessCompleted.class, ProcessCompleted::new);
        CafienneSerializer.addManifestWrapper(ProcessFailed.class, ProcessFailed::new);
        CafienneSerializer.addManifestWrapper(ProcessReactivated.class, ProcessReactivated::new);
        CafienneSerializer.addManifestWrapper(ProcessResumed.class, ProcessResumed::new);
        CafienneSerializer.addManifestWrapper(ProcessSuspended.class, ProcessSuspended::new);
        CafienneSerializer.addManifestWrapper(ProcessTerminated.class, ProcessTerminated::new);
        CafienneSerializer.addManifestWrapper(ProcessModified.class, ProcessModified::new);
        CafienneSerializer.addManifestWrapper(ProcessDefinitionMigrated.class, ProcessDefinitionMigrated::new);
    }

    private static void registerTenantEvents() {
        CafienneSerializer.addManifestWrapper(TenantOwnersRequested.class, TenantOwnersRequested::new);
        CafienneSerializer.addManifestWrapper(TenantModified.class, TenantModified::new);
        CafienneSerializer.addManifestWrapper(TenantAppliedPlatformUpdate.class, TenantAppliedPlatformUpdate::new);
        CafienneSerializer.addManifestWrapper(TenantUserAdded.class, TenantUserAdded::new);
        CafienneSerializer.addManifestWrapper(TenantUserChanged.class, TenantUserChanged::new);
        CafienneSerializer.addManifestWrapper(TenantUserRemoved.class, TenantUserRemoved::new);
        registerDeprecatedTenantEvents();
    }

    private static void registerDeprecatedTenantEvents() {
        CafienneSerializer.addManifestWrapper(TenantUserCreated.class, TenantUserCreated::new);
        CafienneSerializer.addManifestWrapper(TenantUserUpdated.class, TenantUserUpdated::new);
        CafienneSerializer.addManifestWrapper(TenantUserRoleAdded.class, TenantUserRoleAdded::new);
        CafienneSerializer.addManifestWrapper(TenantUserRoleRemoved.class, TenantUserRoleRemoved::new);
        CafienneSerializer.addManifestWrapper(TenantUserEnabled.class, TenantUserEnabled::new);
        CafienneSerializer.addManifestWrapper(TenantUserDisabled.class, TenantUserDisabled::new);
        CafienneSerializer.addManifestWrapper(OwnerAdded.class, OwnerAdded::new);
        CafienneSerializer.addManifestWrapper(OwnerRemoved.class, OwnerRemoved::new);
    }

    private static void registerConsentGroupEvents() {
        CafienneSerializer.addManifestWrapper(ConsentGroupMemberAdded.class, ConsentGroupMemberAdded::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupMemberChanged.class, ConsentGroupMemberChanged::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupMemberRemoved.class, ConsentGroupMemberRemoved::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupCreated.class, ConsentGroupCreated::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupModified.class, ConsentGroupModified::new);
    }

    private static void registerPlatformEvents() {
        CafienneSerializer.addManifestWrapper(TenantCreated.class, TenantCreated::new);
        CafienneSerializer.addManifestWrapper(TenantDisabled.class, TenantDisabled::new);
        CafienneSerializer.addManifestWrapper(TenantEnabled.class, TenantEnabled::new);
    }
}
