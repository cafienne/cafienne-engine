package com.casefabric.querydb.lastmodified

import com.casefabric.querydb.materializer.tenant.TenantReader

case class TenantLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.TENANT_LAST_MODIFIED
  override val registration: LastModifiedRegistration = TenantReader.lastModifiedRegistration
}
