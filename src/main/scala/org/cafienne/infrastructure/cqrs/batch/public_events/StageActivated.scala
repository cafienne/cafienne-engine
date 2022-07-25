/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.plan.{PlanItemCreated, PlanItemTransitioned}
import org.cafienne.cmmn.instance.State
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class StageActivated(identifier: String, name: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.identifier, identifier, Fields.name, name, Fields.caseInstanceId, caseInstanceId)
}

object StageActivated {
  def from(batch: PublicCaseEventBatch): Seq[StageActivated] = {
    batch.stageEvents.filter(_.getType == "Stage").groupBy(p => p.getPlanItemId).flatMap(stageEvents => {
      val stageName = stageEvents._2
        .filter(_.getPlanItemId.equals(stageEvents._1))
        .filter(_.isInstanceOf[PlanItemCreated])
        .map(_.asInstanceOf[PlanItemCreated])
        .headOption.fold("")(p => p.getPlanItemName)

      stageEvents._2
        .filter(_.getPlanItemId.equals(stageEvents._1))
        .filter(_.isInstanceOf[PlanItemTransitioned])
        .map(_.asInstanceOf[PlanItemTransitioned])
        .filter(_.getCurrentState == State.Active)
        .map(event => StageActivated(event.getPlanItemId, stageName, event.getCaseInstanceId))
    }).toSeq
  }

  def deserialize(json: ValueMap): StageActivated = StageActivated(
    identifier = json.readField(Fields.identifier),
    name = json.readField(Fields.name),
    caseInstanceId = json.readField(Fields.caseInstanceId)
  )
}

