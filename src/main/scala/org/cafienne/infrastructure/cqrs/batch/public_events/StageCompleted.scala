/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned
import org.cafienne.cmmn.instance.State
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class StageCompleted(identifier: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.identifier, identifier, Fields.caseInstanceId, caseInstanceId)
}

object StageCompleted {
  def from(batch: PublicCaseEventBatch): Seq[StageCompleted] = {
    batch.stageEvents.filter(_.getType == "Stage").groupBy(p => p.getPlanItemId).flatMap(stageEvents => {
      stageEvents._2
        .filter(_.getPlanItemId.equals(stageEvents._1))
        .filter(_.isInstanceOf[PlanItemTransitioned])
        .map(_.asInstanceOf[PlanItemTransitioned])
        .filter(_.getCurrentState == State.Completed)
        .map(event => StageCompleted(event.getPlanItemId, event.getCaseInstanceId))
    }).toSeq
  }

  def deserialize(json: ValueMap): StageCompleted = StageCompleted(
    identifier = json.readField(Fields.identifier),
    caseInstanceId = json.readField(Fields.caseInstanceId)
  )
}