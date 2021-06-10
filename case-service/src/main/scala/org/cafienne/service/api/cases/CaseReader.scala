package org.cafienne.service.api.cases

import org.cafienne.service.db.materializer.LastModifiedRegistration

object CaseReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Cases")
}
