package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event.team.{TeamMemberAdded, TeamMemberRemoved}
import org.cafienne.service.api.cases.CaseInstanceTeamMember

object CaseInstanceTeamMemberMerger {
    import scala.collection.JavaConverters._

    def merge(event: TeamMemberAdded): Seq[CaseInstanceTeamMember] = {
      event.getRoles.asScala.map {
        role => CaseInstanceTeamMember(caseInstanceId = event.getCaseInstanceId, event.tenant, role = role, userId = event.getUserId, active = true)
      }.toSeq
    }

  def merge(event: TeamMemberRemoved): Seq[CaseInstanceTeamMember] = {
    event.getRoles.asScala.map {
      role => CaseInstanceTeamMember(caseInstanceId = event.getCaseInstanceId, event.tenant, role = role, userId = event.getUserId, active = false)
    }.toSeq
  }

}
