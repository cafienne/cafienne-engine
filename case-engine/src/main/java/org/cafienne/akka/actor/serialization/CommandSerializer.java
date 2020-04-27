package org.cafienne.akka.actor.serialization;

import org.cafienne.cmmn.akka.command.*;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.ReplaceCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.akka.command.debug.SwitchDebugMode;
import org.cafienne.cmmn.akka.command.task.CompleteTask;
import org.cafienne.cmmn.akka.command.task.FailTask;
import org.cafienne.cmmn.akka.command.team.PutTeamMember;
import org.cafienne.cmmn.akka.command.team.RemoveTeamMember;
import org.cafienne.cmmn.akka.command.team.SetCaseTeam;
import org.cafienne.humantask.akka.command.*;
import org.cafienne.processtask.akka.command.*;
import org.cafienne.tenant.akka.command.*;
import org.cafienne.tenant.akka.command.platform.BootstrapTenant;
import org.cafienne.tenant.akka.command.platform.CreateTenant;
import org.cafienne.tenant.akka.command.platform.DisableTenant;
import org.cafienne.tenant.akka.command.platform.EnableTenant;
import org.cafienne.tenant.akka.command.EnableTenantUser;

public class CommandSerializer extends AkkaCaseObjectSerializer {
    static {
        addCaseCommands();
        addHumanTaskCommands();
        addProcessActorCommands();
        addTenantCommands();
        addPlatformCommands();
    }

    private static void addCaseCommands() {
        addManifestWrapper(StartCase.class, StartCase::new);
        addManifestWrapper(AddDiscretionaryItem.class, AddDiscretionaryItem::new);
        addManifestWrapper(GetDiscretionaryItems.class, GetDiscretionaryItems::new);
        addManifestWrapper(MakeCaseTransition.class, MakeCaseTransition::new);
        addManifestWrapper(MakePlanItemTransition.class, MakePlanItemTransition::new);
        addManifestWrapper(CreateCaseFileItem.class, CreateCaseFileItem::new);
        addManifestWrapper(DeleteCaseFileItem.class, DeleteCaseFileItem::new);
        addManifestWrapper(ReplaceCaseFileItem.class, ReplaceCaseFileItem::new);
        addManifestWrapper(UpdateCaseFileItem.class, UpdateCaseFileItem::new);
        addManifestWrapper(SwitchDebugMode.class, SwitchDebugMode::new);
        addManifestWrapper(CompleteTask.class, CompleteTask::new);
        addManifestWrapper(FailTask.class, FailTask::new);
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
        addManifestWrapper(AddTenantUser.class, AddTenantUser::new);
        addManifestWrapper(AddTenantUserRole.class, AddTenantUserRole::new);
        addManifestWrapper(RemoveTenantUserRole.class, RemoveTenantUserRole::new);
        addManifestWrapper(EnableTenantUser.class, EnableTenantUser::new);
        addManifestWrapper(DisableTenantUser.class, DisableTenantUser::new);
        addManifestWrapper(AddTenantOwner.class, AddTenantOwner::new);
        addManifestWrapper(RemoveTenantOwner.class, RemoveTenantOwner::new);
        addManifestWrapper(GetTenantOwners.class, GetTenantOwners::new);
    }

    private static void addPlatformCommands() {
        addManifestWrapper(BootstrapTenant.class, BootstrapTenant::new);
        addManifestWrapper(CreateTenant.class, CreateTenant::new);
        addManifestWrapper(DisableTenant.class, DisableTenant::new);
        addManifestWrapper(EnableTenant.class, EnableTenant::new);
    }
}
