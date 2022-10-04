/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.CaseOutputFilled
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned
import org.cafienne.cmmn.instance.State
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.{Value, ValueMap}

@Manifest
case class CaseCompleted(caseInstanceId: String, output: ValueMap) extends CafiennePublicEventContent {
  override def toValue: Value[_] = new ValueMap(Fields.caseInstanceId, caseInstanceId, Fields.output, output)
}

object CaseCompleted {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[PlanItemTransitioned])
    .filter(_.getType.isCasePlan)
    .filter(_.getCurrentState == State.Completed)
    .map(event => {
      // Read the single case output filled event (there will be only 1) and put it's output into the public event.
      val json = batch.filterMap(classOf[CaseOutputFilled]).map(_.output).headOption.fold(new ValueMap())(_.cloneValueNode())
      PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), CaseCompleted(event.getCaseInstanceId, json))
    })

  def deserialize(json: ValueMap): CaseCompleted = CaseCompleted(caseInstanceId = json.readString(Fields.caseInstanceId), output = json.readMap(Fields.output))
}
