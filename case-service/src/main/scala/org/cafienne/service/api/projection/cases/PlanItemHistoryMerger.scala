package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event._
import org.cafienne.cmmn.instance.PlanItemEvent
import org.cafienne.service.api.cases.PlanItemHistory

object PlanItemHistoryMerger {
  def mapEventToHistory(evt: PlanItemEvent): PlanItemHistory = {
    evt match {
      case event: PlanItemCreated =>
        PlanItemHistory(
          id = event.getId,
          planItemId = event.getPlanItemId,
          stageId = event.stageId,
          name = event.planItemName,
          index = event.index,
          caseInstanceId = event.getCaseInstanceId(),
          tenant = event.tenant,
          planItemType = event.getType(),
          lastModified = event.createdOn,
          modifiedBy = event.getUser.id,
          eventType = event.getClass.getName,
          sequenceNr = event.getSequenceNumber
        )
      case event: PlanItemTransitioned =>
        PlanItemHistory(
          id = event.getId,
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId(),
          index = event.index,
          tenant = event.tenant,
          historyState = event.getHistoryState().toString,
          currentState = event.getCurrentState.toString,
          transition = event.getTransition.toString,
          lastModified = null,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName,
          sequenceNr = evt.getSequenceNumber
        )
      case event: RepetitionRuleEvaluated =>
        PlanItemHistory(
          id = event.getId,
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId(),
          index = event.index,
          tenant = event.tenant,
          repeating = event.isRepeating,
          lastModified = null,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName,
          sequenceNr = evt.getSequenceNumber
        )
      case event: RequiredRuleEvaluated =>
        PlanItemHistory(
          id = event.getId,
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId(),
          index = event.index,
          tenant = event.tenant,
          required = event.isRequired,
          lastModified = null,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName,
          sequenceNr = evt.getSequenceNumber
        )
    }
  }
  def merge(modified: CaseModified, current: PlanItemHistory): PlanItemHistory =
    current.copy(lastModified = modified.lastModified(), modifiedBy = modified.getUser.id)
}
