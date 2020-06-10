package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event._
import org.cafienne.cmmn.akka.event.plan.{PlanItemCreated, PlanItemTransitioned, RepetitionRuleEvaluated, RequiredRuleEvaluated}
import org.cafienne.cmmn.akka.event.plan.task.{TaskInputFilled, TaskOutputFilled}
import org.cafienne.service.api.cases.table.PlanItemRecord

object PlanItemMerger {

  def merge(event: PlanItemCreated): PlanItemRecord = {
    PlanItemRecord(
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

  def merge(event: CaseModified, current: PlanItemRecord): PlanItemRecord =
    current.copy(
      lastModified = event.lastModified(),
      modifiedBy = event.getUser.id)

  def merge(event: PlanItemTransitioned, current: PlanItemRecord): PlanItemRecord =
    current.copy(
      transition = event.getTransition.toString,
      currentState = event.getCurrentState.toString,
      historyState = event.getHistoryState.toString,
    )

  def merge(event: RepetitionRuleEvaluated, current: PlanItemRecord): PlanItemRecord =
    current.copy(repeating = event.isRepeating)

  def merge(event: RequiredRuleEvaluated, current: PlanItemRecord): PlanItemRecord =
    current.copy(required = event.isRequired)

  def merge(event: TaskInputFilled, current: PlanItemRecord): PlanItemRecord =
    current.copy(
      taskInput = event.getTaskInputParameters.toString,
      mappedInput = event.getMappedInputParameters.toString
    )

  def merge(event: TaskOutputFilled, current: PlanItemRecord): PlanItemRecord =
    current.copy(
      taskOutput = event.getTaskOutputParameters.toString,
      rawOutput = event.getRawOutputParameters.toString
    )
}
