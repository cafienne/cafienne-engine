package org.cafienne.service.api.projection.cases

import org.cafienne.cmmn.akka.event.file.{BusinessIdentifierCleared, BusinessIdentifierSet}
import org.cafienne.service.api.projection.record.CaseBusinessIdentifierRecord

object CaseIdentifierMerger {

  def merge(event: BusinessIdentifierSet): CaseBusinessIdentifierRecord = {
    CaseBusinessIdentifierRecord(event.getActorId, event.tenant, event.name, Some(event.value.getValue.toString), true, event.path.toString)
  }

  def merge(event: BusinessIdentifierCleared): CaseBusinessIdentifierRecord = {
    CaseBusinessIdentifierRecord(event.getActorId, event.tenant, event.name, None, false, event.path.toString)
  }
}
