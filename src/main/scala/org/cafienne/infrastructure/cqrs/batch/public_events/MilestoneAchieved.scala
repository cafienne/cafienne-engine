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

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned
import org.cafienne.cmmn.instance.{Path, State}
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class MilestoneAchieved(milestoneId: String, path: Path, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.milestoneId, milestoneId, Fields.path, path, Fields.caseInstanceId, caseInstanceId)
  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object MilestoneAchieved {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[PlanItemTransitioned])
    .filter(_.getCurrentState == State.Completed)
    .filter(_.getType.isMilestone)
    .map(event => PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), MilestoneAchieved(event.getPlanItemId, event.path, event.getCaseInstanceId)))

  def deserialize(json: ValueMap): MilestoneAchieved = MilestoneAchieved(
    milestoneId = json.readString(Fields.milestoneId),
    path = json.readPath(Fields.path),
    caseInstanceId = json.readString(Fields.caseInstanceId)
  )
}