package org.cafienne.service.db.materializer.tenant

import org.cafienne.service.db.materializer.LastModifiedRegistration

object TenantReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Tenants")
}
