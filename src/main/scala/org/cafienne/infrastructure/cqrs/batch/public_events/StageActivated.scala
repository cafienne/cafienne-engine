/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned
import org.cafienne.cmmn.instance.{Path, State}
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class StageActivated(stageId: String, parentStage: String, path: Path, name: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.stageId, stageId, Fields.parentStage, parentStage, Fields.path, path, Fields.name, name, Fields.caseInstanceId, caseInstanceId)

  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object StageActivated {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[PlanItemTransitioned])
    .filter(_.getCurrentState == State.Active)
    .filter(_.getType.isStage)
    .map(event => PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), StageActivated(event.getPlanItemId, event.stageId, event.path, event.path.name, event.getCaseInstanceId)))

  def deserialize(json: ValueMap): StageActivated = StageActivated(
    stageId = json.readString(Fields.stageId),
    parentStage = json.readString(Fields.parentStage),
    path = json.readPath(Fields.path),
    name = json.readString(Fields.name),
    caseInstanceId = json.readString(Fields.caseInstanceId)
  )
}

