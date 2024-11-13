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

package com.casefabric.infrastructure.cqrs.batch.public_events

import com.casefabric.cmmn.actorapi.event.CaseDefinitionApplied
import com.casefabric.cmmn.actorapi.event.file.CaseFileItemTransitioned
import com.casefabric.infrastructure.serialization.{Fields, Manifest}
import com.casefabric.json.ValueMap
import com.casefabric.querydb.materializer.cases.file.CaseFileMerger

@Manifest
case class CaseStarted(caseInstanceId: String, caseName: String, parentCaseId: String, rootCaseId: String, caseFile: ValueMap) extends CaseFabricPublicEventContent {
  override def toValue: ValueMap = new ValueMap(
    Fields.caseInstanceId, caseInstanceId,
    Fields.caseName, caseName,
    Fields.parentCaseId, parentCaseId,
    Fields.rootCaseId, rootCaseId,
    Fields.file, caseFile)
}

object CaseStarted {
  def from(batch: PublicCaseEventBatch): Seq[PublicEventWrapper] = batch
    .filterMap(classOf[CaseDefinitionApplied])
    .map(event => {
      val caseInstanceId = event.getCaseInstanceId
      val caseName = event.getCaseName
      val parentCaseId = event.getParentCaseId
      val rootCaseId = event.getRootCaseId
      val caseFile = readCaseFile(batch.filterMap(classOf[CaseFileItemTransitioned]))
      PublicEventWrapper(batch.timestamp, batch.getSequenceNr(event), CaseStarted(caseInstanceId = caseInstanceId, caseName = caseName, parentCaseId = parentCaseId, rootCaseId = rootCaseId, caseFile = caseFile))
    })

  def readCaseFile(caseFileEvents: Seq[CaseFileItemTransitioned]): ValueMap = {
    ValueMap.fill(map => caseFileEvents.foreach(event => CaseFileMerger.merge(event, map)))
  }

  def deserialize(json: ValueMap): CaseStarted = new CaseStarted(
    caseInstanceId = json.readString(Fields.caseInstanceId),
    caseName = json.readString(Fields.caseName),
    parentCaseId = json.readString(Fields.parentCaseId),
    rootCaseId = json.readString(Fields.rootCaseId),
    caseFile = json.readMap(Fields.file)
  )
}
