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

package com.casefabric.infrastructure.cqrs.batch.public_events

import com.casefabric.cmmn.actorapi.event.plan.PlanItemTransitioned
import com.casefabric.cmmn.instance.{Path, State}
import com.casefabric.infrastructure.serialization.{Fields, Manifest}
import com.casefabric.json.{Value, ValueMap}

@Manifest
case class StageCompleted(stageId: String, path: Path, caseInstanceId: String) extends CaseFabricPublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.stageId, stageId, Fields.path, path, Fields.caseInstanceId, caseInstanceId)

  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object StageCompleted {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
      .filterMap(classOf[PlanItemTransitioned])
      .filter(_.getCurrentState == State.Completed)
      .filter(_.getType.isStage)
      .map(event => PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), StageCompleted(event.getPlanItemId, event.path, event.getCaseInstanceId)))

  def deserialize(json: ValueMap): StageCompleted = StageCompleted(
    stageId = json.readString(Fields.stageId),
    path = json.readPath(Fields.path),
    caseInstanceId = json.readString(Fields.caseInstanceId)
  )
}