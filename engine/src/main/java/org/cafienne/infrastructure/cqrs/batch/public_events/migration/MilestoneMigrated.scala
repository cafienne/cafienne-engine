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

package org.cafienne.infrastructure.cqrs.batch.public_events.migration

import org.cafienne.cmmn.actorapi.event.migration.PlanItemMigrated
import org.cafienne.cmmn.instance.Path
import org.cafienne.infrastructure.cqrs.batch.public_events.{CafiennePublicEventContent, PublicCaseEventBatch, PublicEventWrapper}
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class MilestoneMigrated(milestoneId: String, parentStage: String, path: Path, name: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.milestoneId, milestoneId, Fields.parentStage, parentStage, Fields.path, path, Fields.name, name, Fields.caseInstanceId, caseInstanceId)
  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object MilestoneMigrated {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[PlanItemMigrated])
    .filter(_.getType.isMilestone)
    .map(event => PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), MilestoneMigrated(event.getPlanItemId, event.stageId, event.path, event.path.name, event.getCaseInstanceId)))

  def deserialize(json: ValueMap): MilestoneMigrated = MilestoneMigrated(
    milestoneId = json.readString(Fields.milestoneId),
    parentStage = json.readString(Fields.parentStage),
    path = json.readPath(Fields.path),
    name = json.readString(Fields.name),
    caseInstanceId = json.readString(Fields.caseInstanceId)
  )
}
