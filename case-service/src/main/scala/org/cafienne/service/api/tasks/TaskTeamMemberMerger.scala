package org.cafienne.service.api.tasks

import org.cafienne.cmmn.akka.event.team._

object TaskTeamMemberMerger {

  import scala.collection.JavaConverters._

  def merge(event: TeamMemberAdded): Seq[TaskTeamMemberRecord] = {
    (event.getRoles.asScala ++ Seq("")).map {
      role => TaskTeamMemberRecord(caseInstanceId = event.getCaseInstanceId, event.tenant, caseRole = role, memberId = event.getUserId, isTenantUser = true, isOwner = true, active = true)
    }.toSeq
  }

  def merge(event: TeamMemberRemoved): Seq[TaskTeamMemberRecord] = {
    (event.getRoles.asScala ++ Seq("")).map {
      role => TaskTeamMemberRecord(caseInstanceId = event.getCaseInstanceId, event.tenant, caseRole = role, memberId = event.getUserId, isTenantUser = true, isOwner = false, active = false)
    }.toSeq
  }

  def merge(event: TeamRoleFilled): Seq[TaskTeamMemberRecord] = {
    Seq(TaskTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = event.roleName, isTenantUser = event.isTenantUser, isOwner = false, active = true))
  }

  def merge(event: TeamRoleCleared): Seq[TaskTeamMemberRecord] = {
    Seq(TaskTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = event.roleName, isTenantUser = event.isTenantUser, isOwner = false, active = false))
  }

  def merge(event: CaseOwnerAdded): Seq[TaskTeamMemberRecord] = {
    Seq(TaskTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = "", isTenantUser = event.isTenantUser, isOwner = true, active = true))
  }

  def merge(event: CaseOwnerRemoved): Seq[TaskTeamMemberRecord] = {
    Seq(TaskTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = "", isTenantUser = event.isTenantUser, isOwner = false, active = true))
  }
}
