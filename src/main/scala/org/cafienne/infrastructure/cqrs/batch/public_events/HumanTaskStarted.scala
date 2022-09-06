/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated
import org.cafienne.humantask.actorapi.event.{HumanTaskActivated, HumanTaskEvent, HumanTaskInputSaved}
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{JSONReader, StringValue, Value, ValueMap}

@Manifest
case class HumanTaskStarted(taskId: String, taskName: String, caseInstanceId: String, stageId: String, inputParameters: ValueMap, form: Value[_]) extends CafiennePublicEventContent {
  override def toValue: ValueMap = new ValueMap(
    Fields.taskId, taskId,
    Fields.taskName, taskName,
    Fields.caseInstanceId, caseInstanceId,
    Fields.inputParameters, inputParameters,
    Fields.stageId, stageId,
    EFields.form, form)
}

object HumanTaskStarted {
  def deserialize(json: ValueMap): HumanTaskStarted = HumanTaskStarted(
    taskId = json.readField(Fields.taskId),
    taskName = json.readField(Fields.taskName),
    caseInstanceId = json.readField(Fields.caseInstanceId),
    stageId = Option(json.readField(Fields.stageId)).orNull,
    inputParameters = json.readMap(Fields.inputParameters),
    form = json.get(EFields.form)
  )

  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[HumanTaskActivated])
    .map(event => {
      val events = batch.filterMap(classOf[HumanTaskEvent]).filter(_.taskId == event.taskId)
      val taskId = event.taskId
      val taskName = event.getTaskName
      val caseInstanceId = event.getCaseInstanceId
      val inputParameters = events.find(_.isInstanceOf[HumanTaskInputSaved]).get.asInstanceOf[HumanTaskInputSaved].getInput
      val taskModel = event.getTaskModel
      val stageId = batch.filterMap(classOf[PlanItemCreated]).find(_.getPlanItemId == taskId).map(_.stageId).orNull
      val form: Value[_] = try {
        JSONReader.parse(taskModel)
      } catch {
        // If not a JSON document, then render it as a plain string (possibly empty)
        // Note: perhaps we should fix this in the base event itself, as that converts the json to a string ...
        case _: Throwable => new StringValue(taskModel)
      }
      PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), HumanTaskStarted(taskId = taskId, taskName = taskName, caseInstanceId = caseInstanceId, stageId = stageId, inputParameters = inputParameters, form = form))
    })
}
