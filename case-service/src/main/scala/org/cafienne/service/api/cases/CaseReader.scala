package org.cafienne.service.api.cases

import org.cafienne.service.api.projection.LastModifiedRegistration

object CaseReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Cases")
}
