/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.instance.Path
import org.cafienne.humantask.actorapi.event.{HumanTaskActivated, HumanTaskEvent, HumanTaskInputSaved}
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
      val events = batch.filterMap(classOf[HumanTaskEvent]).filter(_.taskId == event.taskId)
      val taskId = event.taskId
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
