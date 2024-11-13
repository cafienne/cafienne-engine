package com.casefabric.querydb.lastmodified

import com.casefabric.querydb.materializer.cases.CaseReader

case class CaseLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.CASE_LAST_MODIFIED
  override val registration: LastModifiedRegistration = CaseReader.lastModifiedRegistration
}
