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

import com.casefabric.cmmn.actorapi.command.ReactivateCase;
import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import com.casefabric.cmmn.actorapi.command.casefile.DeleteCaseFileItem;
import com.casefabric.cmmn.actorapi.command.casefile.ReplaceCaseFileItem;
import com.casefabric.cmmn.actorapi.command.casefile.UpdateCaseFileItem;
import com.casefabric.cmmn.actorapi.command.debug.SwitchDebugMode;
import com.casefabric.cmmn.actorapi.command.migration.MigrateCaseDefinition;
import com.casefabric.cmmn.actorapi.command.migration.MigrateDefinition;
import com.casefabric.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import com.casefabric.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import com.casefabric.cmmn.actorapi.command.plan.MakeCaseTransition;
import com.casefabric.cmmn.actorapi.command.plan.MakePlanItemTransition;
import com.casefabric.cmmn.actorapi.command.plan.eventlistener.RaiseEvent;
import com.casefabric.cmmn.actorapi.command.plan.task.CompleteTask;
import com.casefabric.cmmn.actorapi.command.plan.task.FailTask;
import com.casefabric.cmmn.actorapi.command.plan.task.HandleTaskImplementationTransition;
import com.casefabric.cmmn.actorapi.command.team.DeprecatedUpsert;
import com.casefabric.cmmn.actorapi.command.team.SetCaseTeam;
import com.casefabric.cmmn.actorapi.command.team.removemember.RemoveCaseTeamGroup;
import com.casefabric.cmmn.actorapi.command.team.removemember.RemoveCaseTeamTenantRole;
import com.casefabric.cmmn.actorapi.command.team.removemember.RemoveCaseTeamUser;
import com.casefabric.cmmn.actorapi.command.team.setmember.SetCaseTeamGroup;
import com.casefabric.cmmn.actorapi.command.team.setmember.SetCaseTeamTenantRole;
import com.casefabric.cmmn.actorapi.command.team.setmember.SetCaseTeamUser;
import com.casefabric.consentgroup.actorapi.command.CreateConsentGroup;
import com.casefabric.consentgroup.actorapi.command.RemoveConsentGroupMember;
import com.casefabric.consentgroup.actorapi.command.ReplaceConsentGroup;
import com.casefabric.consentgroup.actorapi.command.SetConsentGroupMember;
import com.casefabric.humantask.actorapi.command.*;
import com.casefabric.infrastructure.serialization.CaseFabricSerializer;
import com.casefabric.processtask.actorapi.command.*;
import com.casefabric.tenant.actorapi.command.GetTenantOwners;
import com.casefabric.tenant.actorapi.command.RemoveTenantUser;
import com.casefabric.tenant.actorapi.command.ReplaceTenant;
import com.casefabric.tenant.actorapi.command.SetTenantUser;
import com.casefabric.tenant.actorapi.command.platform.CreateTenant;
import com.casefabric.tenant.actorapi.command.platform.DisableTenant;
import com.casefabric.tenant.actorapi.command.platform.EnableTenant;

public class CommandSerializers {
    public static void register() {
        addCaseCommands();
        addProcessActorCommands();
        addTenantCommands();
        addConsentGroupCommands();
        addPlatformCommands();
    }

    private static void addCaseCommands() {
        CaseFabricSerializer.addManifestWrapper(StartCase.class, StartCase::new);
        CaseFabricSerializer.addManifestWrapper(ReactivateCase.class, ReactivateCase::new);
        CaseFabricSerializer.addManifestWrapper(MigrateDefinition.class, MigrateDefinition::new);
        CaseFabricSerializer.addManifestWrapper(MigrateCaseDefinition.class, MigrateCaseDefinition::new);
        CaseFabricSerializer.addManifestWrapper(SwitchDebugMode.class, SwitchDebugMode::new);
        addCasePlanCommands();
        addCaseFileCommands();
        addCaseTeamCommands();
        addHumanTaskCommands();
    }

    private static void addCasePlanCommands() {
        CaseFabricSerializer.addManifestWrapper(AddDiscretionaryItem.class, AddDiscretionaryItem::new);
        CaseFabricSerializer.addManifestWrapper(GetDiscretionaryItems.class, GetDiscretionaryItems::new);
        CaseFabricSerializer.addManifestWrapper(MakeCaseTransition.class, MakeCaseTransition::new);
        CaseFabricSerializer.addManifestWrapper(MakePlanItemTransition.class, MakePlanItemTransition::new);
        CaseFabricSerializer.addManifestWrapper(HandleTaskImplementationTransition.class, HandleTaskImplementationTransition::new);
        CaseFabricSerializer.addManifestWrapper(CompleteTask.class, CompleteTask::new);
        CaseFabricSerializer.addManifestWrapper(FailTask.class, FailTask::new);
        CaseFabricSerializer.addManifestWrapper(RaiseEvent.class, RaiseEvent::new);
    }

    private static void addCaseFileCommands() {
        CaseFabricSerializer.addManifestWrapper(CreateCaseFileItem.class, CreateCaseFileItem::new);
        CaseFabricSerializer.addManifestWrapper(DeleteCaseFileItem.class, DeleteCaseFileItem::new);
        CaseFabricSerializer.addManifestWrapper(ReplaceCaseFileItem.class, ReplaceCaseFileItem::new);
        CaseFabricSerializer.addManifestWrapper(UpdateCaseFileItem.class, UpdateCaseFileItem::new);
    }

    private static void addCaseTeamCommands() {
        CaseFabricSerializer.addManifestWrapper(DeprecatedUpsert.class, DeprecatedUpsert::new);
        CaseFabricSerializer.addManifestWrapper(SetCaseTeamUser.class, SetCaseTeamUser::new);
        CaseFabricSerializer.addManifestWrapper(SetCaseTeamTenantRole.class, SetCaseTeamTenantRole::new);
        CaseFabricSerializer.addManifestWrapper(SetCaseTeamGroup.class, SetCaseTeamGroup::new);
        CaseFabricSerializer.addManifestWrapper(RemoveCaseTeamUser.class, RemoveCaseTeamUser::new);
        CaseFabricSerializer.addManifestWrapper(RemoveCaseTeamGroup.class, RemoveCaseTeamGroup::new);
        CaseFabricSerializer.addManifestWrapper(RemoveCaseTeamTenantRole.class, RemoveCaseTeamTenantRole::new);
        CaseFabricSerializer.addManifestWrapper(SetCaseTeam.class, SetCaseTeam::new);
    }

    private static void addHumanTaskCommands() {
        CaseFabricSerializer.addManifestWrapper(AssignTask.class, AssignTask::new);
        CaseFabricSerializer.addManifestWrapper(ClaimTask.class, ClaimTask::new);
        CaseFabricSerializer.addManifestWrapper(CompleteHumanTask.class, CompleteHumanTask::new);
        CaseFabricSerializer.addManifestWrapper(DelegateTask.class, DelegateTask::new);
        CaseFabricSerializer.addManifestWrapper(FillTaskDueDate.class, FillTaskDueDate::new);
        CaseFabricSerializer.addManifestWrapper(RevokeTask.class, RevokeTask::new);
        CaseFabricSerializer.addManifestWrapper(SaveTaskOutput.class, SaveTaskOutput::new);
        CaseFabricSerializer.addManifestWrapper(ValidateTaskOutput.class, ValidateTaskOutput::new);
    }

    private static void addProcessActorCommands() {
        CaseFabricSerializer.addManifestWrapper(StartProcess.class, StartProcess::new);
        CaseFabricSerializer.addManifestWrapper(ResumeProcess.class, ResumeProcess::new);
        CaseFabricSerializer.addManifestWrapper(ReactivateProcess.class, ReactivateProcess::new);
        CaseFabricSerializer.addManifestWrapper(SuspendProcess.class, SuspendProcess::new);
        CaseFabricSerializer.addManifestWrapper(TerminateProcess.class, TerminateProcess::new);
        CaseFabricSerializer.addManifestWrapper(MigrateProcessDefinition.class, MigrateProcessDefinition::new);
    }

    private static void addTenantCommands() {
        CaseFabricSerializer.addManifestWrapper(SetTenantUser.class, SetTenantUser::new);
        CaseFabricSerializer.addManifestWrapper(RemoveTenantUser.class, RemoveTenantUser::new);
        CaseFabricSerializer.addManifestWrapper(GetTenantOwners.class, GetTenantOwners::new);
        CaseFabricSerializer.addManifestWrapper(ReplaceTenant.class, ReplaceTenant::new);
    }

    private static void addConsentGroupCommands() {
        CaseFabricSerializer.addManifestWrapper(CreateConsentGroup.class, CreateConsentGroup::new);
        CaseFabricSerializer.addManifestWrapper(ReplaceConsentGroup.class, ReplaceConsentGroup::new);
        CaseFabricSerializer.addManifestWrapper(SetConsentGroupMember.class, SetConsentGroupMember::new);
        CaseFabricSerializer.addManifestWrapper(RemoveConsentGroupMember.class, RemoveConsentGroupMember::new);
    }

    private static void addPlatformCommands() {
        CaseFabricSerializer.addManifestWrapper(CreateTenant.class, CreateTenant::new);
        CaseFabricSerializer.addManifestWrapper(DisableTenant.class, DisableTenant::new);
        CaseFabricSerializer.addManifestWrapper(EnableTenant.class, EnableTenant::new);
    }
}
