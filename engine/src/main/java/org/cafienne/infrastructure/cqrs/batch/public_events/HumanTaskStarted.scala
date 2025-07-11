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

import org.cafienne.engine.cmmn.instance.Path
import org.cafienne.engine.humantask.actorapi.event.{HumanTaskActivated, HumanTaskEvent, HumanTaskInputSaved}
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{JSONReader, StringValue, Value, ValueMap}

@Manifest
case class HumanTaskStarted(taskId: String, path: Path, taskName: String, caseInstanceId: String, parentStage: String, inputParameters: ValueMap, form: Value[_]) extends CafiennePublicEventContent {
  override def toValue: ValueMap = new ValueMap(
    Fields.taskId, taskId,
    Fields.path, path,
    Fields.taskName, taskName,
    Fields.caseInstanceId, caseInstanceId,
    Fields.inputParameters, inputParameters,
    Fields.parentStage, parentStage,
    Fields.form, form)

  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object HumanTaskStarted {
  def deserialize(json: ValueMap): HumanTaskStarted = HumanTaskStarted(
    taskId = json.readString(Fields.taskId),
    path = json.readPath(Fields.path),
    taskName = json.readString(Fields.taskName),
    caseInstanceId = json.readString(Fields.caseInstanceId),
    parentStage = json.readString(Fields.parentStage),
    inputParameters = json.readMap(Fields.inputParameters),
    form = json.get(Fields.form)
  )

  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[HumanTaskActivated])
    .map(event => {
      val events = batch.filterMap(classOf[HumanTaskEvent]).filter(_.getTaskId == event.getTaskId)
      val taskId = event.getTaskId
      val path = event.path
      val taskName = event.getTaskName
      val caseInstanceId = event.getCaseInstanceId
      val stageId = event.stageId
      val inputParameters = events.find(_.isInstanceOf[HumanTaskInputSaved]).get.asInstanceOf[HumanTaskInputSaved].getInput
      val taskModel = event.getTaskModel
      val form: Value[_] = try {
        JSONReader.parse(taskModel)
      } catch {
        // If not a JSON document, then render it as a plain string (possibly empty)
        // Note: perhaps we should fix this in the base event itself, as that converts the json to a string ...
        case _: Throwable => new StringValue(taskModel)
      }
      PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), HumanTaskStarted(taskId = taskId, path = path, taskName = taskName, caseInstanceId = caseInstanceId, parentStage = stageId, inputParameters = inputParameters, form = form))
    })
}
