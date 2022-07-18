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
case class CaseCompleted(caseInstanceId: String) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.caseInstanceId, caseInstanceId)
}

object CaseCompleted {
  def from(batch: PublicCaseEventBatch): Seq[CaseCompleted] = {
    batch.filterMap(classOf[PlanItemTransitioned])
      .filter(_.getType == "CasePlan")
      .filter(_.getCurrentState == State.Completed)
      .map(event => CaseCompleted(event.getCaseInstanceId))
  }

  def deserialize(json: ValueMap): CaseCompleted = CaseCompleted(caseInstanceId = json.readField(Fields.caseInstanceId))
}
