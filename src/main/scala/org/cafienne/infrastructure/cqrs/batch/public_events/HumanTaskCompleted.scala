/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.instance.Path
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class HumanTaskCompleted(taskId: String, path: Path, taskName: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.taskId, taskId, Fields.path, path, Fields.taskName, taskName, Fields.caseInstanceId, caseInstanceId)
  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object HumanTaskCompleted {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[org.cafienne.humantask.actorapi.event.HumanTaskCompleted])
    .map(event => PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), HumanTaskCompleted(event.getTaskId, event.path, event.getTaskName, event.getCaseInstanceId)))

  def deserialize(json: ValueMap): HumanTaskCompleted = HumanTaskCompleted(
    taskId = json.readString(Fields.taskId),
    path = json.readPath(Fields.path),
    taskName = json.readString(Fields.taskName),
    caseInstanceId = json.readString(Fields.caseInstanceId)
  )
}
