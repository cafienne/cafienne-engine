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

package com.casefabric.infrastructure.cqrs.batch.public_events.migration

import com.casefabric.cmmn.instance.Path
import com.casefabric.infrastructure.cqrs.batch.public_events.{CaseFabricPublicEventContent, PublicCaseEventBatch, PublicEventWrapper}
import com.casefabric.infrastructure.serialization.{Fields, Manifest}
import com.casefabric.json.ValueMap

@Manifest
case class HumanTaskDropped(taskId: String, path: Path, taskName: String, caseInstanceId: String, parentStage: String) extends CaseFabricPublicEventContent {
  override def toValue: ValueMap = new ValueMap(
    Fields.taskId, taskId,
    Fields.path, path,
    Fields.taskName, taskName,
    Fields.caseInstanceId, caseInstanceId,
    Fields.parentStage, parentStage)

  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object HumanTaskDropped {
  def deserialize(json: ValueMap): HumanTaskDropped = HumanTaskDropped(taskId = json.readString(Fields.taskId), path = json.readPath(Fields.path), taskName = json.readString(Fields.taskName), caseInstanceId = json.readString(Fields.caseInstanceId), parentStage = json.readString(Fields.parentStage))

  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[com.casefabric.humantask.actorapi.event.migration.HumanTaskDropped])
    .map(event => {
      val taskId = event.getTaskId
      val path = event.path
      val taskName = event.getTaskName
      val caseInstanceId = event.getCaseInstanceId
      val stageId = event.stageId
      PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), HumanTaskDropped(taskId = taskId, path = path, taskName = taskName, caseInstanceId = caseInstanceId, parentStage = stageId))
    })
}
