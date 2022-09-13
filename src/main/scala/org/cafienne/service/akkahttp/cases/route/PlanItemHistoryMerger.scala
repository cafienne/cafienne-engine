package org.cafienne.service.akkahttp.cases.route

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.migration.PlanItemMigrated
import org.cafienne.cmmn.actorapi.event.plan._
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.querydb.record.PlanItemHistoryRecord

object PlanItemHistoryMerger extends LazyLogging {
  def mapModelEventEnvelope(evt: ModelEventEnvelope): Option[PlanItemHistoryRecord] = {
    mapEventToHistory(evt.event.asInstanceOf[CasePlanEvent])
  }

  def mapEventToHistory(evt: CasePlanEvent): Option[PlanItemHistoryRecord] = {
    evt match {
      case event: PlanItemCreated =>
        Some(PlanItemHistoryRecord(
          id = event.getId,
          planItemId = event.getPlanItemId,
          stageId = event.stageId,
          name = event.planItemName,
          index = event.index,
          caseInstanceId = event.getCaseInstanceId,
          tenant = event.tenant,
          planItemType = event.getType.toString,
          lastModified = event.createdOn,
          modifiedBy = event.getUser.id,
          eventType = event.getClass.getName,
          sequenceNr = event.getSequenceNumber
        ))
      case event: PlanItemTransitioned =>
        Some(PlanItemHistoryRecord(
          id = event.getId,
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.index,
          tenant = event.tenant,
          historyState = event.getHistoryState.toString,
          currentState = event.getCurrentState.toString,
          transition = event.getTransition.toString,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName,
          sequenceNr = evt.getSequenceNumber
        ))
      case event: RepetitionRuleEvaluated =>
        Some(PlanItemHistoryRecord(
          id = event.getId,
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.index,
          tenant = event.tenant,
          repeating = event.isRepeating,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName,
          sequenceNr = evt.getSequenceNumber
        ))
      case event: RequiredRuleEvaluated =>
        Some(PlanItemHistoryRecord(
          id = event.getId,
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.index,
          tenant = event.tenant,
          required = event.isRequired,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName,
          sequenceNr = evt.getSequenceNumber
        ))
      case event: PlanItemMigrated =>
        Some(PlanItemHistoryRecord(
          id = event.getId,
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.index,
          tenant = event.tenant,
          name = event.planItemName,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName,
          sequenceNr = evt.getSequenceNumber
        ))
      case _ => None // No interest in other case plan events at this moment
    }
  }
}
