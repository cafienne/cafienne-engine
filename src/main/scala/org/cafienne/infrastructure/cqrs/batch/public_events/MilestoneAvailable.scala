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
case class MilestoneAvailable(identifier: String, name: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.identifier, identifier, Fields.name, name, Fields.caseInstanceId, caseInstanceId)
}

object MilestoneAvailable {
  def from(batch: PublicCaseEventBatch): Seq[MilestoneAvailable] = {
    val planItemName = batch.filterMap(classOf[PlanItemCreated])
      .filter(_.getType == "Milestone")
      .map(event => event.getPlanItemName).headOption.getOrElse("")

    batch.filterMap(classOf[PlanItemTransitioned])
      .filter(_.getType == "Milestone")
      .filter(_.getCurrentState == State.Available)
      .map(event => MilestoneAvailable(event.getPlanItemId, planItemName, event.getCaseInstanceId))

  }

  def deserialize(json: ValueMap): MilestoneAvailable = MilestoneAvailable(
    identifier = json.readField(Fields.identifier),
    name = json.readField(Fields.name),
    caseInstanceId = json.readField(Fields.caseInstanceId)
  )
}
