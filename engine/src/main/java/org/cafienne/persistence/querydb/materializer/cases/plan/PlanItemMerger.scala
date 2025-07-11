/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.querydb.materializer.cases.plan

import org.cafienne.engine.cmmn.actorapi.event.CaseModified
import org.cafienne.engine.cmmn.actorapi.event.migration.PlanItemMigrated
import org.cafienne.engine.cmmn.actorapi.event.plan.task.{TaskInputFilled, TaskOutputFilled}
import org.cafienne.engine.cmmn.actorapi.event.plan.{PlanItemCreated, PlanItemTransitioned, RepetitionRuleEvaluated, RequiredRuleEvaluated}
import org.cafienne.persistence.querydb.record.PlanItemRecord

object PlanItemMerger {

  def merge(event: PlanItemCreated): PlanItemRecord = {
    PlanItemRecord(
      id = event.getPlanItemId,
      definitionId = event.definitionId,
      stageId = event.stageId,
      name = event.getPlanItemName,
      index = event.getIndex,
      caseInstanceId = event.getCaseInstanceId,
      tenant = event.tenant,
      planItemType = event.getType.toString,
      lastModified = event.getCreatedOn,
      modifiedBy = event.getUser.id,
      createdOn = event.getCreatedOn,
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

  def merge(event: PlanItemMigrated, current: PlanItemRecord): PlanItemRecord =
    current.copy(
      name = event.planItemName,
      definitionId = event.definitionId,
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
