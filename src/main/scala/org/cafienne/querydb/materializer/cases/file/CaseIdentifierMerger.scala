package org.cafienne.querydb.materializer.cases.file

import org.cafienne.cmmn.actorapi.event.file.{BusinessIdentifierCleared, BusinessIdentifierSet}
import org.cafienne.querydb.record.CaseBusinessIdentifierRecord

object CaseIdentifierMerger {

  def merge(event: BusinessIdentifierSet): CaseBusinessIdentifierRecord = {
    CaseBusinessIdentifierRecord(event.getActorId, event.tenant, event.name, Some(event.value.getValue.toString), true, event.path.toString)
  }

  def merge(event: BusinessIdentifierCleared): CaseBusinessIdentifierRecord = {
    CaseBusinessIdentifierRecord(event.getActorId, event.tenant, event.name, None, false, event.path.toString)
  }
}
