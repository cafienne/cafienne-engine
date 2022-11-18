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
          planItemId = event.getPlanItemId,
          stageId = event.stageId,
          name = event.getPlanItemName,
          index = event.getIndex,
          caseInstanceId = event.getCaseInstanceId,
          tenant = event.tenant,
          planItemType = event.getType.toString,
          lastModified = event.getCreatedOn,
          modifiedBy = event.getUser.id,
          eventType = event.getClass.getName
        ))
      case event: PlanItemTransitioned =>
        Some(PlanItemHistoryRecord(
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.getIndex,
          tenant = event.tenant,
          historyState = event.getHistoryState.toString,
          currentState = event.getCurrentState.toString,
          transition = event.getTransition.toString,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName
        ))
      case event: RepetitionRuleEvaluated =>
        Some(PlanItemHistoryRecord(
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.getIndex,
          tenant = event.tenant,
          repeating = event.isRepeating,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName
        ))
      case event: RequiredRuleEvaluated =>
        Some(PlanItemHistoryRecord(
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.getIndex,
          tenant = event.tenant,
          required = event.isRequired,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName
        ))
      case event: PlanItemMigrated =>
        Some(PlanItemHistoryRecord(
          planItemId = event.getPlanItemId,
          caseInstanceId = event.getCaseInstanceId,
          index = event.getIndex,
          tenant = event.tenant,
          name = event.planItemName,
          lastModified = event.getTimestamp,
          modifiedBy = event.getUser.id,
          eventType = evt.getClass.getName
        ))
      case _ => None // No interest in other case plan events at this moment
    }
  }
}
