package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event.CaseDefinitionApplied
import org.cafienne.service.api.cases.CaseInstanceRole

object CaseInstanceRoleMerger {

  import scala.collection.JavaConverters._

  def merge(event: CaseDefinitionApplied): Seq[CaseInstanceRole] = {
    val caseDefinition = event.getDefinition()
    caseDefinition.getCaseRoles().asScala.map(role => CaseInstanceRole(event.getCaseInstanceId, event.tenant, role.getName, assigned = false)).toSeq
  }

}
