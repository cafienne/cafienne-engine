/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated
import org.cafienne.humantask.actorapi.event.{HumanTaskActivated, HumanTaskInputSaved}
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{JSONReader, StringValue, Value, ValueMap}

@Manifest
case class HumanTaskStarted(taskId: String, taskName: String, caseInstanceId: String, stageId:String, inputParameters: ValueMap, form: Value[_]) extends CafiennePublicEventContent {
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
    stageId = Option(json.readField(Fields.stageId)).getOrElse(null),
    inputParameters = json.readMap(Fields.inputParameters),
    form = json.get(EFields.form)
  )

  def from(batch: PublicCaseEventBatch): Seq[HumanTaskStarted] = {
    batch.humanTaskEvents.filter(_.isInstanceOf[HumanTaskActivated]).map(_.asInstanceOf[HumanTaskActivated]).map(taskActivation => {
      val events = batch.humanTaskEvents.filter(_.taskId == taskActivation.taskId)
      val taskId = taskActivation.taskId
      val taskName = taskActivation.getTaskName
      val caseInstanceId = taskActivation.getCaseInstanceId
      val inputParameters = events.find(_.isInstanceOf[HumanTaskInputSaved]).get.asInstanceOf[HumanTaskInputSaved].getInput
      val taskModel = taskActivation.getTaskModel
      val stageId = batch.planItemEvents
        .filter(_.getPlanItemId.equals(taskId))
        .filter(_.isInstanceOf[PlanItemCreated])
        .filter(_.getType.equals("HumanTask"))
        .map(_.asInstanceOf[PlanItemCreated])
        .headOption.map(_.stageId).getOrElse(null)
      val form: Value[_] = try {
        JSONReader.parse(taskModel)
      } catch {
        // If not a JSON document, then render it as a plain string (possibly empty)
        // Note: perhaps we should fix this in the base event itself, as that converts the json to a string ...
        case _: Throwable => new StringValue(taskModel)
      }
      HumanTaskStarted(taskId = taskId, taskName = taskName, caseInstanceId = caseInstanceId, stageId = stageId, inputParameters = inputParameters, form = form)
    })
  }
}
