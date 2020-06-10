package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event.team.{TeamMemberAdded, TeamMemberRemoved}
import org.cafienne.service.api.cases.table.CaseTeamMemberRecord

object CaseInstanceTeamMemberMerger {

  import scala.collection.JavaConverters._

  def merge(event: TeamMemberAdded): Seq[CaseTeamMemberRecord] = {
    (event.getRoles.asScala ++ Seq("")).map {
      role => CaseTeamMemberRecord(caseInstanceId = event.getCaseInstanceId, event.tenant, caseRole = role, memberId = event.getUserId, isTenantUser = true, isOwner = false, active = true)
    }.toSeq
  }

  def merge(event: TeamMemberRemoved): Seq[CaseTeamMemberRecord] = {
    (event.getRoles.asScala ++ Seq("")).map {
      role => CaseTeamMemberRecord(caseInstanceId = event.getCaseInstanceId, event.tenant, caseRole = role, memberId = event.getUserId, isTenantUser = true, isOwner = false, active = false)
    }.toSeq
  }

}
