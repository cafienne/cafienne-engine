package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event.{CaseDefinitionApplied, CaseModified}
import org.cafienne.cmmn.instance.State
import org.cafienne.service.api.cases.table.CaseRecord

object CaseInstanceMerger {

  def merge(evt: CaseDefinitionApplied): CaseRecord = {
    CaseRecord(
      id = evt.getCaseInstanceId,
      tenant = evt.tenant,
      rootCaseId = evt.getRootCaseId,
      parentCaseId = evt.getParentCaseId,
      name = evt.getCaseName,
      state = State.Active.toString, // Will always be overridden from CaseModified event
      failures = 0,
      lastModified = evt.createdOn,
      modifiedBy = evt.createdBy,
      createdBy = evt.createdBy,
      createdOn = evt.createdOn
    )
  }

  def merge(evt: CaseModified, currentCaseInstance: CaseRecord): CaseRecord = {
    currentCaseInstance.copy(
      lastModified = evt.lastModified,
      modifiedBy = evt.getUser.id,
      failures = evt.getNumFailures,
      state = evt.getState.toString)
  }
}
