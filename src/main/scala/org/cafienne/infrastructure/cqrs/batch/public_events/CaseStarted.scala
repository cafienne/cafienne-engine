/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.cmmn.actorapi.event.CaseDefinitionApplied
import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap
import org.cafienne.querydb.materializer.cases.file.CaseFileMerger

@Manifest
case class CaseStarted(caseInstanceId: String, caseName: String, parentCaseId: String, rootCaseId: String, caseFile: ValueMap) extends CafiennePublicEventContent {
  override def toValue: ValueMap = new ValueMap(
    Fields.caseInstanceId, caseInstanceId,
    Fields.caseName, caseName,
    EFields.parentCaseId, parentCaseId,
    EFields.rootCaseId, rootCaseId,
    EFields.file, caseFile)
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
    caseInstanceId = json.readField(Fields.caseInstanceId),
    caseName = json.readField(Fields.caseName),
    parentCaseId = json.read(EFields.parentCaseId).getValue.toString,
    rootCaseId = json.read(EFields.rootCaseId).getValue.toString,
    caseFile = json.read(EFields.file).asMap()
  )
}
