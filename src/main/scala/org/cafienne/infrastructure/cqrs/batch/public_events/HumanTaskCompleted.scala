/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class HumanTaskCompleted(taskId: String, taskName: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.taskId, taskId, Fields.taskName, taskName, Fields.caseInstanceId, caseInstanceId)
}

object HumanTaskCompleted {
  def from(batch: PublicCaseEventBatch): Seq[HumanTaskCompleted] = {
    batch.filterMap(classOf[org.cafienne.humantask.actorapi.event.HumanTaskCompleted]).map(event => HumanTaskCompleted(event.taskId, event.getTaskName, event.getCaseInstanceId))
  }

  def deserialize(json: ValueMap): HumanTaskCompleted = HumanTaskCompleted(
    taskId = json.readField(Fields.taskId),
    taskName = json.readField(Fields.taskName),
    caseInstanceId = json.readField(Fields.caseInstanceId)
  )
}