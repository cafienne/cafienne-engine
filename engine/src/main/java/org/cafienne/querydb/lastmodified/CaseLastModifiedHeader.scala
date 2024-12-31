package org.cafienne.querydb.lastmodified

import org.cafienne.querydb.materializer.cases.CaseReader

case class CaseLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.CASE_LAST_MODIFIED
  override val registration: LastModifiedRegistration = CaseReader.lastModifiedRegistration
}
