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
case class MilestoneAchieved(identifier: String, name: String, caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.identifier, identifier, Fields.name, name, Fields.caseInstanceId, caseInstanceId)
}

object MilestoneAchieved {
  def from(batch: PublicCaseEventBatch): Seq[MilestoneAchieved] = {
    Seq()
  }

  def deserialize(json: ValueMap): MilestoneAchieved = MilestoneAchieved(
    identifier = json.readField(Fields.identifier),
    name = json.readField(Fields.name),
    caseInstanceId = json.readField(Fields.caseInstanceId)
  )
}