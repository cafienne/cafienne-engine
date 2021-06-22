package org.cafienne.service.db.materializer.cases

import org.cafienne.cmmn.actorapi.event.CaseDefinitionApplied
import org.cafienne.service.db.record.CaseRoleRecord

object CaseInstanceRoleMerger {

  import scala.collection.JavaConverters._

  def merge(event: CaseDefinitionApplied): Seq[CaseRoleRecord] = {
    val caseDefinition = event.getDefinition()
    caseDefinition.getCaseTeamModel().getCaseRoles().asScala.map(role => CaseRoleRecord(event.getCaseInstanceId, event.tenant, role.getName, assigned = false)).toSeq
  }

}
