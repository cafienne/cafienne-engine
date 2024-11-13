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

package com.casefabric.infrastructure.cqrs.batch.public_events.migration

import com.casefabric.cmmn.actorapi.event.migration.CaseDefinitionMigrated
import com.casefabric.infrastructure.cqrs.batch.public_events.{CaseFabricPublicEventContent, PublicCaseEventBatch, PublicEventWrapper}
import com.casefabric.infrastructure.serialization.{Fields, Manifest}
import com.casefabric.json.ValueMap

@Manifest
case class CaseMigrated(caseInstanceId: String, caseName: String) extends CaseFabricPublicEventContent {
  override def toValue: ValueMap = new ValueMap(
    Fields.caseInstanceId, caseInstanceId,
    Fields.caseName, caseName)
}

object CaseMigrated {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[CaseDefinitionMigrated])
    .map(event => {
      val caseInstanceId = event.getCaseInstanceId
      val caseName = event.getCaseName
      PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), CaseMigrated(caseInstanceId = caseInstanceId, caseName = caseName))
    })

  def deserialize(json: ValueMap): CaseMigrated = new CaseMigrated(caseInstanceId = json.readString(Fields.caseInstanceId), caseName = json.readString(Fields.caseName))
}
