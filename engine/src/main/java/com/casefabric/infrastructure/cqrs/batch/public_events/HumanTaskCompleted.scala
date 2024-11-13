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

import com.casefabric.cmmn.actorapi.event.plan.task.TaskOutputFilled
import com.casefabric.cmmn.instance.Path
import com.casefabric.infrastructure.serialization.{Fields, Manifest}
import com.casefabric.json.{Value, ValueMap}

@Manifest
case class HumanTaskCompleted(taskId: String, path: Path, taskName: String, caseInstanceId: String, output: ValueMap) extends CaseFabricPublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.taskId, taskId, Fields.path, path, Fields.taskName, taskName, Fields.caseInstanceId, caseInstanceId, Fields.output, output)

  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object HumanTaskCompleted {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[com.casefabric.humantask.actorapi.event.HumanTaskCompleted])
    .map(event => {
      val output = batch.filterMap(classOf[TaskOutputFilled]).filter(_.getTaskId == event.getTaskId).head.getTaskOutputParameters
      PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), HumanTaskCompleted(event.getTaskId, event.path, event.getTaskName, event.getCaseInstanceId, output))
    })

  def deserialize(json: ValueMap): HumanTaskCompleted = HumanTaskCompleted(
    taskId = json.readString(Fields.taskId),
    path = json.readPath(Fields.path),
    taskName = json.readString(Fields.taskName),
    caseInstanceId = json.readString(Fields.caseInstanceId),
    output = json.readMap(Fields.output)
  )
}
