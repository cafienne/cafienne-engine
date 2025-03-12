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

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.reply.event.ActorRequestExecuted;
import org.cafienne.actormodel.communication.reply.event.ActorRequestFailed;
import org.cafienne.actormodel.communication.reply.event.ActorRequestStored;
import org.cafienne.actormodel.communication.reply.command.RunActorRequest;
import org.cafienne.actormodel.communication.request.event.ActorRequestCreated;
import org.cafienne.actormodel.communication.request.response.ActorRequestFailure;
import org.cafienne.actormodel.communication.request.response.ActorRequestDeliveryReceipt;
import org.cafienne.actormodel.communication.request.event.ActorRequestDelivered;
import org.cafienne.actormodel.communication.request.command.RequestModelActor;
import org.cafienne.cmmn.actorapi.command.ReactivateCase;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.ReplaceCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.debug.SwitchDebugMode;
import org.cafienne.cmmn.actorapi.command.migration.MigrateCaseDefinition;
import org.cafienne.cmmn.actorapi.command.migration.MigrateDefinition;
import org.cafienne.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import org.cafienne.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import org.cafienne.cmmn.actorapi.command.plan.MakeCaseTransition;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.plan.eventlistener.RaiseEvent;
import org.cafienne.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.cmmn.actorapi.command.plan.task.FailTask;
import org.cafienne.cmmn.actorapi.command.plan.task.HandleTaskImplementationTransition;
import org.cafienne.cmmn.actorapi.command.team.DeprecatedUpsert;
import org.cafienne.cmmn.actorapi.command.team.SetCaseTeam;
import org.cafienne.cmmn.actorapi.command.team.removemember.RemoveCaseTeamGroup;
import org.cafienne.cmmn.actorapi.command.team.removemember.RemoveCaseTeamTenantRole;
import org.cafienne.cmmn.actorapi.command.team.removemember.RemoveCaseTeamUser;
import org.cafienne.cmmn.actorapi.command.team.setmember.SetCaseTeamGroup;
import org.cafienne.cmmn.actorapi.command.team.setmember.SetCaseTeamTenantRole;
import org.cafienne.cmmn.actorapi.command.team.setmember.SetCaseTeamUser;
import org.cafienne.consentgroup.actorapi.command.CreateConsentGroup;
import org.cafienne.consentgroup.actorapi.command.RemoveConsentGroupMember;
import org.cafienne.consentgroup.actorapi.command.ReplaceConsentGroup;
import org.cafienne.consentgroup.actorapi.command.SetConsentGroupMember;
import org.cafienne.humantask.actorapi.command.*;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.*;
import org.cafienne.tenant.actorapi.command.GetTenantOwners;
import org.cafienne.tenant.actorapi.command.RemoveTenantUser;
import org.cafienne.tenant.actorapi.command.ReplaceTenant;
import org.cafienne.tenant.actorapi.command.SetTenantUser;
import org.cafienne.tenant.actorapi.command.platform.CreateTenant;
import org.cafienne.tenant.actorapi.command.platform.DisableTenant;
import org.cafienne.tenant.actorapi.command.platform.EnableTenant;

public class CommandSerializers {
    public static <T extends ModelCommand> T deserializeModelCommand(ValueMap json) {
        String manifest = json.readString(Fields.manifest);
        return (T) new CafienneSerializer().fromJson(json.readMap(Fields.content), manifest);
    }

    public static void register() {
        CafienneSerializer.addManifestWrapper(RequestModelActor.class, RequestModelActor::new);
        CafienneSerializer.addManifestWrapper(RunActorRequest.class, RunActorRequest::new);
        CafienneSerializer.addManifestWrapper(ActorRequestCreated.class, ActorRequestCreated::new);
        CafienneSerializer.addManifestWrapper(ActorRequestDelivered.class, ActorRequestDelivered::new);
        CafienneSerializer.addManifestWrapper(ActorRequestDeliveryReceipt.class, ActorRequestDeliveryReceipt::new);
        CafienneSerializer.addManifestWrapper(ActorRequestStored.class, ActorRequestStored::new);
        CafienneSerializer.addManifestWrapper(ActorRequestFailure.class, ActorRequestFailure::new);
        CafienneSerializer.addManifestWrapper(ActorRequestExecuted.class, ActorRequestExecuted::new);
        CafienneSerializer.addManifestWrapper(ActorRequestFailed.class, ActorRequestFailed::new);
        addCaseCommands();
        addProcessActorCommands();
        addTenantCommands();
        addConsentGroupCommands();
        addPlatformCommands();
    }

    private static void addCaseCommands() {
        CafienneSerializer.addManifestWrapper(StartCase.class, StartCase::new);
        CafienneSerializer.addManifestWrapper(ReactivateCase.class, ReactivateCase::new);
        CafienneSerializer.addManifestWrapper(MigrateDefinition.class, MigrateDefinition::new);
        CafienneSerializer.addManifestWrapper(MigrateCaseDefinition.class, MigrateCaseDefinition::new);
        CafienneSerializer.addManifestWrapper(SwitchDebugMode.class, SwitchDebugMode::new);
        addCasePlanCommands();
        addCaseFileCommands();
        addCaseTeamCommands();
        addHumanTaskCommands();
    }

    private static void addCasePlanCommands() {
        CafienneSerializer.addManifestWrapper(AddDiscretionaryItem.class, AddDiscretionaryItem::new);
        CafienneSerializer.addManifestWrapper(GetDiscretionaryItems.class, GetDiscretionaryItems::new);
        CafienneSerializer.addManifestWrapper(MakeCaseTransition.class, MakeCaseTransition::new);
        CafienneSerializer.addManifestWrapper(MakePlanItemTransition.class, MakePlanItemTransition::new);
        CafienneSerializer.addManifestWrapper(HandleTaskImplementationTransition.class, HandleTaskImplementationTransition::new);
        CafienneSerializer.addManifestWrapper(CompleteTask.class, CompleteTask::new);
        CafienneSerializer.addManifestWrapper(FailTask.class, FailTask::new);
        CafienneSerializer.addManifestWrapper(RaiseEvent.class, RaiseEvent::new);
    }

    private static void addCaseFileCommands() {
        CafienneSerializer.addManifestWrapper(CreateCaseFileItem.class, CreateCaseFileItem::new);
        CafienneSerializer.addManifestWrapper(DeleteCaseFileItem.class, DeleteCaseFileItem::new);
        CafienneSerializer.addManifestWrapper(ReplaceCaseFileItem.class, ReplaceCaseFileItem::new);
        CafienneSerializer.addManifestWrapper(UpdateCaseFileItem.class, UpdateCaseFileItem::new);
    }

    private static void addCaseTeamCommands() {
        CafienneSerializer.addManifestWrapper(DeprecatedUpsert.class, DeprecatedUpsert::new);
        CafienneSerializer.addManifestWrapper(SetCaseTeamUser.class, SetCaseTeamUser::new);
        CafienneSerializer.addManifestWrapper(SetCaseTeamTenantRole.class, SetCaseTeamTenantRole::new);
        CafienneSerializer.addManifestWrapper(SetCaseTeamGroup.class, SetCaseTeamGroup::new);
        CafienneSerializer.addManifestWrapper(RemoveCaseTeamUser.class, RemoveCaseTeamUser::new);
        CafienneSerializer.addManifestWrapper(RemoveCaseTeamGroup.class, RemoveCaseTeamGroup::new);
        CafienneSerializer.addManifestWrapper(RemoveCaseTeamTenantRole.class, RemoveCaseTeamTenantRole::new);
        CafienneSerializer.addManifestWrapper(SetCaseTeam.class, SetCaseTeam::new);
    }

    private static void addHumanTaskCommands() {
        CafienneSerializer.addManifestWrapper(AssignTask.class, AssignTask::new);
        CafienneSerializer.addManifestWrapper(ClaimTask.class, ClaimTask::new);
        CafienneSerializer.addManifestWrapper(CompleteHumanTask.class, CompleteHumanTask::new);
        CafienneSerializer.addManifestWrapper(DelegateTask.class, DelegateTask::new);
        CafienneSerializer.addManifestWrapper(FillTaskDueDate.class, FillTaskDueDate::new);
        CafienneSerializer.addManifestWrapper(RevokeTask.class, RevokeTask::new);
        CafienneSerializer.addManifestWrapper(SaveTaskOutput.class, SaveTaskOutput::new);
        CafienneSerializer.addManifestWrapper(ValidateTaskOutput.class, ValidateTaskOutput::new);
    }

    private static void addProcessActorCommands() {
        CafienneSerializer.addManifestWrapper(StartProcess.class, StartProcess::new);
        CafienneSerializer.addManifestWrapper(ResumeProcess.class, ResumeProcess::new);
        CafienneSerializer.addManifestWrapper(ReactivateProcess.class, ReactivateProcess::new);
        CafienneSerializer.addManifestWrapper(SuspendProcess.class, SuspendProcess::new);
        CafienneSerializer.addManifestWrapper(TerminateProcess.class, TerminateProcess::new);
        CafienneSerializer.addManifestWrapper(MigrateProcessDefinition.class, MigrateProcessDefinition::new);
    }

    private static void addTenantCommands() {
        CafienneSerializer.addManifestWrapper(SetTenantUser.class, SetTenantUser::new);
        CafienneSerializer.addManifestWrapper(RemoveTenantUser.class, RemoveTenantUser::new);
        CafienneSerializer.addManifestWrapper(GetTenantOwners.class, GetTenantOwners::new);
        CafienneSerializer.addManifestWrapper(ReplaceTenant.class, ReplaceTenant::new);
    }

    private static void addConsentGroupCommands() {
        CafienneSerializer.addManifestWrapper(CreateConsentGroup.class, CreateConsentGroup::new);
        CafienneSerializer.addManifestWrapper(ReplaceConsentGroup.class, ReplaceConsentGroup::new);
        CafienneSerializer.addManifestWrapper(SetConsentGroupMember.class, SetConsentGroupMember::new);
        CafienneSerializer.addManifestWrapper(RemoveConsentGroupMember.class, RemoveConsentGroupMember::new);
    }

    private static void addPlatformCommands() {
        CafienneSerializer.addManifestWrapper(CreateTenant.class, CreateTenant::new);
        CafienneSerializer.addManifestWrapper(DisableTenant.class, DisableTenant::new);
        CafienneSerializer.addManifestWrapper(EnableTenant.class, EnableTenant::new);
    }
}
