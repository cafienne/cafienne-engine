package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event.team._
import org.cafienne.service.api.projection.record.CaseTeamMemberRecord

object CaseInstanceTeamMemberMerger {

  import scala.collection.JavaConverters._

  def merge(event: TeamMemberAdded): Seq[CaseTeamMemberRecord] = {
    (event.getRoles.asScala ++ Seq("")).map {
      role => CaseTeamMemberRecord(caseInstanceId = event.getCaseInstanceId, event.tenant, caseRole = role, memberId = event.getUserId, isTenantUser = true, isOwner = true, active = true)
    }.toSeq
  }

  def merge(event: TeamMemberRemoved): Seq[CaseTeamMemberRecord] = {
    (event.getRoles.asScala ++ Seq("")).map {
      role => CaseTeamMemberRecord(caseInstanceId = event.getCaseInstanceId, event.tenant, caseRole = role, memberId = event.getUserId, isTenantUser = true, isOwner = false, active = false)
    }.toSeq
  }

  def merge(event: TeamRoleFilled): Seq[CaseTeamMemberRecord] = {
    Seq(CaseTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = event.roleName, isTenantUser = event.isTenantUser, isOwner = false, active = true))
  }

  def merge(event: TeamRoleCleared): Seq[CaseTeamMemberRecord] = {
    Seq(CaseTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = event.roleName, isTenantUser = event.isTenantUser, isOwner = false, active = false))
  }

  def merge(event: CaseOwnerAdded): Seq[CaseTeamMemberRecord] = {
    Seq(CaseTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = "", isTenantUser = event.isTenantUser, isOwner = true, active = true))
  }

  def merge(event: CaseOwnerRemoved): Seq[CaseTeamMemberRecord] = {
    Seq(CaseTeamMemberRecord(caseInstanceId = event.getActorId, tenant = event.tenant, memberId = event.memberId, caseRole = "", isTenantUser = event.isTenantUser, isOwner = false, active = true))
  }
}
