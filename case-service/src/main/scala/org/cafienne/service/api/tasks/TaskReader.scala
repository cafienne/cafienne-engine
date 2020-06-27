package org.cafienne.service.api.tasks

import org.cafienne.cmmn.akka.event.CaseModified
import org.cafienne.service.api.projection.LastModifiedRegistration

object TaskReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Tasks")

  def inform(caseModified: CaseModified) = {
    lastModifiedRegistration.handle(caseModified)
  }
}

