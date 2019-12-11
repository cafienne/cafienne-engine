package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event.{CaseDefinitionApplied, CaseModified}
import org.cafienne.cmmn.instance.State
import org.cafienne.service.api.cases.CaseInstance

object CaseInstanceMerger {

  def merge(evt: CaseDefinitionApplied): CaseInstance = {
    CaseInstance(
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

  def merge(evt: CaseModified, currentCaseInstance: CaseInstance): CaseInstance = {
    currentCaseInstance.copy(
      lastModified = evt.lastModified,
      modifiedBy = evt.getUser.id,
      failures = evt.getNumFailures,
      state = evt.getState.toString)
  }
}
