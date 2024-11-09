/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
