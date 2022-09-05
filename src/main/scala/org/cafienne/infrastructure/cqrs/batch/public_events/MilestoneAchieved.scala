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
case class MilestoneAchieved(identifier: String, path: Path, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.identifier, identifier, Fields.path, path, Fields.caseInstanceId, caseInstanceId)
  override def toString: String = getClass.getSimpleName + "[" + path + "]"
}

object MilestoneAchieved {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[PlanItemTransitioned])
    .filter(_.getCurrentState == State.Completed)
    .filter(_.getType == "Milestone")
    .map(event => PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), MilestoneAchieved(event.getPlanItemId, event.path, event.getCaseInstanceId)))

  def deserialize(json: ValueMap): MilestoneAchieved = MilestoneAchieved(
    identifier = json.readField(Fields.identifier),
    path = json.readPath(Fields.path),
    caseInstanceId = json.readField(Fields.caseInstanceId)
  )
}