package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event._
import org.cafienne.service.api.cases.PlanItem

object PlanItemMerger {

  def merge(event: PlanItemCreated): PlanItem = {
    PlanItem(
      id = event.getPlanItemId,
      stageId = event.stageId,
      name = event.planItemName,
      index = event.index,
      caseInstanceId = event.getCaseInstanceId,
      tenant = event.tenant,
      planItemType = event.getType,
      lastModified = event.createdOn,
      modifiedBy = event.getUser.id,
      createdOn = event.createdOn,
      createdBy = event.getUser.id)
  }

  def merge(event: CaseModified, current: PlanItem): PlanItem =
    current.copy(
      lastModified = event.lastModified(),
      modifiedBy = event.getUser.id)

  def merge(event: PlanItemTransitioned, current: PlanItem): PlanItem =
    current.copy(
      transition = event.getTransition.toString,
      currentState = event.getCurrentState.toString,
      historyState = event.getHistoryState.toString,
    )

  def merge(event: RepetitionRuleEvaluated, current: PlanItem): PlanItem =
    current.copy(repeating = event.isRepeating)

  def merge(event: RequiredRuleEvaluated, current: PlanItem): PlanItem =
    current.copy(required = event.isRequired)

  def merge(event: TaskInputFilled, current: PlanItem): PlanItem =
    current.copy(
      taskInput = Some(event.getTaskInputParameters.toString),
      mappedInput = Some(event.getMappedInputParameters.toString)
    )

  def merge(event: TaskOutputFilled, current: PlanItem): PlanItem =
    current.copy(
      taskOutput = Some(event.getTaskOutputParameters.toString),
      rawOutput = Some(event.getRawOutputParameters.toString)
    )
}
