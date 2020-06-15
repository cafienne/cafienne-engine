package org.cafienne.service.api.cases

import org.cafienne.cmmn.akka.event.CaseModified
import org.cafienne.service.api.projection.LastModifiedRegistration

object CaseReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Cases")

  def inform(caseModified: CaseModified) = {
    lastModifiedRegistration.handle(caseModified)
  }
}
