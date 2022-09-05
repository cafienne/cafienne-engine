/*
 * Copyright (C) 2022 Batav B.V. <https://www.batav.com/cafienne-enterprise>
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.instance.Path
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class HumanTaskTerminated(taskId: String, path: Path, taskName: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.taskId, taskId, Fields.path, path, Fields.taskName, taskName, Fields.caseInstanceId, caseInstanceId)
  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object HumanTaskTerminated {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[org.cafienne.humantask.actorapi.event.HumanTaskTerminated])
    .map(event => PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), HumanTaskTerminated(event.taskId, event.path, event.getTaskName, event.getCaseInstanceId)))

  def deserialize(json: ValueMap): HumanTaskTerminated = HumanTaskTerminated(
    taskId = json.readField(Fields.taskId),
    path = json.readPath(Fields.path),
    taskName = json.readField(Fields.taskName),
    caseInstanceId = json.readField(Fields.caseInstanceId)
  )
}