package org.cafienne.actormodel.serialization;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.UpdateCaseWithPlatformInformation;
import org.cafienne.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.ReplaceCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.debug.SwitchDebugMode;
import org.cafienne.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import org.cafienne.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import org.cafienne.cmmn.actorapi.command.plan.MakeCaseTransition;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.plan.eventlistener.RaiseEvent;
import org.cafienne.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.cmmn.actorapi.command.plan.task.FailTask;
import org.cafienne.cmmn.actorapi.command.team.PutTeamMember;
import org.cafienne.cmmn.actorapi.command.team.RemoveTeamMember;
import org.cafienne.cmmn.actorapi.command.team.SetCaseTeam;
import org.cafienne.humantask.actorapi.command.*;
import org.cafienne.platform.actorapi.command.GetUpdateStatus;
import org.cafienne.platform.actorapi.command.UpdatePlatformInformation;
import org.cafienne.processtask.actorapi.command.*;
import org.cafienne.tenant.actorapi.command.*;
import org.cafienne.tenant.actorapi.command.platform.CreateTenant;
import org.cafienne.tenant.actorapi.command.platform.DisableTenant;
import org.cafienne.tenant.actorapi.command.platform.EnableTenant;
import org.cafienne.tenant.actorapi.command.platform.UpdateTenantWithPlatformInformation;

public class CommandSerializer extends CafienneSerializer {
    public static void register() {
        addCaseCommands();
        addProcessActorCommands();
        addTenantCommands();
        addPlatformCommands();
    }

    private static void addCaseCommands() {
        addManifestWrapper(StartCase.class, StartCase::new);
        addManifestWrapper(SwitchDebugMode.class, SwitchDebugMode::new);
        addManifestWrapper(UpdateCaseWithPlatformInformation.class, UpdateCaseWithPlatformInformation::new);
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
        addManifestWrapper(PutTeamMember.class, PutTeamMember::new);
        addManifestWrapper(RemoveTeamMember.class, RemoveTeamMember::new);
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
        addManifestWrapper(UpdateTenantWithPlatformInformation.class, UpdateTenantWithPlatformInformation::new);
    }

    private static void addPlatformCommands() {
        addManifestWrapper(CreateTenant.class, CreateTenant::new);
        addManifestWrapper(DisableTenant.class, DisableTenant::new);
        addManifestWrapper(EnableTenant.class, EnableTenant::new);
        addManifestWrapper(UpdatePlatformInformation.class, UpdatePlatformInformation::new);
        addManifestWrapper(GetUpdateStatus.class, GetUpdateStatus::new);
    }
}
