package org.cafienne.infrastructure.serialization;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.ReplaceCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.debug.SwitchDebugMode;
import org.cafienne.cmmn.actorapi.command.migration.MigrateDefinition;
import org.cafienne.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import org.cafienne.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import org.cafienne.cmmn.actorapi.command.plan.MakeCaseTransition;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.plan.eventlistener.RaiseEvent;
import org.cafienne.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.cmmn.actorapi.command.plan.task.FailTask;
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
import org.cafienne.consentgroup.actorapi.command.SetConsentGroupMember;
import org.cafienne.humantask.actorapi.command.*;
import org.cafienne.processtask.actorapi.command.*;
import org.cafienne.tenant.actorapi.command.*;
import org.cafienne.tenant.actorapi.command.platform.CreateTenant;
import org.cafienne.tenant.actorapi.command.platform.DisableTenant;
import org.cafienne.tenant.actorapi.command.platform.EnableTenant;

public class CommandSerializer extends CafienneSerializer {
    public static void register() {
        addCaseCommands();
        addProcessActorCommands();
        addTenantCommands();
        addConsentGroupCommands();
        addPlatformCommands();
    }

    private static void addCaseCommands() {
        addManifestWrapper(StartCase.class, StartCase::new);
        addManifestWrapper(MigrateDefinition.class, MigrateDefinition::new);
        addManifestWrapper(SwitchDebugMode.class, SwitchDebugMode::new);
        addCasePlanCommands();
        addCaseFileCommands();
        addCaseTeamCommands();
        addHumanTaskCommands();
    }

    private static void addCasePlanCommands() {
        addManifestWrapper(AddDiscretionaryItem.class, AddDiscretionaryItem::new);
        addManifestWrapper(GetDiscretionaryItems.class, GetDiscretionaryItems::new);
        addManifestWrapper(MakeCaseTransition.class, MakeCaseTransition::new);
        addManifestWrapper(MakePlanItemTransition.class, MakePlanItemTransition::new);
        addManifestWrapper(CompleteTask.class, CompleteTask::new);
        addManifestWrapper(FailTask.class, FailTask::new);
        addManifestWrapper(RaiseEvent.class, RaiseEvent::new);
    }

    private static void addCaseFileCommands() {
        addManifestWrapper(CreateCaseFileItem.class, CreateCaseFileItem::new);
        addManifestWrapper(DeleteCaseFileItem.class, DeleteCaseFileItem::new);
        addManifestWrapper(ReplaceCaseFileItem.class, ReplaceCaseFileItem::new);
        addManifestWrapper(UpdateCaseFileItem.class, UpdateCaseFileItem::new);
    }

    private static void addCaseTeamCommands() {
        addManifestWrapper(DeprecatedUpsert.class, DeprecatedUpsert::new);
        addManifestWrapper(SetCaseTeamUser.class, SetCaseTeamUser::new);
        addManifestWrapper(SetCaseTeamTenantRole.class, SetCaseTeamTenantRole::new);
        addManifestWrapper(SetCaseTeamGroup.class, SetCaseTeamGroup::new);
        addManifestWrapper(RemoveCaseTeamUser.class, RemoveCaseTeamUser::new);
        addManifestWrapper(RemoveCaseTeamGroup.class, RemoveCaseTeamGroup::new);
        addManifestWrapper(RemoveCaseTeamTenantRole.class, RemoveCaseTeamTenantRole::new);
        addManifestWrapper(SetCaseTeam.class, SetCaseTeam::new);
    }

    private static void addHumanTaskCommands() {
        addManifestWrapper(AssignTask.class, AssignTask::new);
        addManifestWrapper(ClaimTask.class, ClaimTask::new);
        addManifestWrapper(CompleteHumanTask.class, CompleteHumanTask::new);
        addManifestWrapper(DelegateTask.class, DelegateTask::new);
        addManifestWrapper(FillTaskDueDate.class, FillTaskDueDate::new);
        addManifestWrapper(RevokeTask.class, RevokeTask::new);
        addManifestWrapper(SaveTaskOutput.class, SaveTaskOutput::new);
        addManifestWrapper(ValidateTaskOutput.class, ValidateTaskOutput::new);
    }

    private static void addProcessActorCommands() {
        addManifestWrapper(StartProcess.class, StartProcess::new);
        addManifestWrapper(ResumeProcess.class, ResumeProcess::new);
        addManifestWrapper(ReactivateProcess.class, ReactivateProcess::new);
        addManifestWrapper(SuspendProcess.class, SuspendProcess::new);
        addManifestWrapper(TerminateProcess.class, TerminateProcess::new);
    }

    private static void addTenantCommands() {
        addManifestWrapper(UpsertTenantUser.class, UpsertTenantUser::new);
        addManifestWrapper(UpdateTenantUser.class, UpdateTenantUser::new);
        addManifestWrapper(ReplaceTenantUser.class, ReplaceTenantUser::new);
        addManifestWrapper(AddTenantUserRole.class, AddTenantUserRole::new);
        addManifestWrapper(RemoveTenantUserRole.class, RemoveTenantUserRole::new);
        addManifestWrapper(GetTenantOwners.class, GetTenantOwners::new);
        addManifestWrapper(ReplaceTenant.class, ReplaceTenant::new);
        addManifestWrapper(UpdateTenant.class, UpdateTenant::new);
    }

    private static void addConsentGroupCommands() {
        CafienneSerializer.addManifestWrapper(CreateConsentGroup.class, CreateConsentGroup::new);
        CafienneSerializer.addManifestWrapper(SetConsentGroupMember.class, SetConsentGroupMember::new);
        CafienneSerializer.addManifestWrapper(RemoveConsentGroupMember.class, RemoveConsentGroupMember::new);
    }

    private static void addPlatformCommands() {
        addManifestWrapper(CreateTenant.class, CreateTenant::new);
        addManifestWrapper(DisableTenant.class, DisableTenant::new);
        addManifestWrapper(EnableTenant.class, EnableTenant::new);
    }
}
