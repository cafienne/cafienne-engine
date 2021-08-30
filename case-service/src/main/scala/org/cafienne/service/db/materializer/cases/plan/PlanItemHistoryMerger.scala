package org.cafienne.service.db.materializer.cases.plan

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.TransactionEvent
import org.cafienne.cmmn.actorapi.event.plan._
import org.cafienne.service.db.record.PlanItemHistoryRecord

object PlanItemHistoryMerger extends LazyLogging {
  def mapEventToHistory(evt: PlanItemEvent): Option[PlanItemHistoryRecord] = {
    evt match {
      case event: PlanItemCreated =>
        Some(PlanItemHistoryRecord(
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
        ))
      case event: PlanItemTransitioned =>
        Some(PlanItemHistoryRecord(
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
        ))
      case event: RepetitionRuleEvaluated =>
        Some(PlanItemHistoryRecord(
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
        ))
      case event: RequiredRuleEvaluated =>
        Some(PlanItemHistoryRecord(
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
        ))
      case other =>
        logger.warn(s"Cannot handle plan item events of type ${evt.getClass.getName}")
        None
    }
  }

  def merge(modified: TransactionEvent[_], current: PlanItemHistoryRecord): PlanItemHistoryRecord =
    current.copy(lastModified = modified.lastModified(), modifiedBy = modified.getUser.id)
}
