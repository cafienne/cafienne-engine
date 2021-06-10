package org.cafienne.service.api.tenant

import org.cafienne.service.db.materializer.LastModifiedRegistration

object TenantReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Tenants")
}
