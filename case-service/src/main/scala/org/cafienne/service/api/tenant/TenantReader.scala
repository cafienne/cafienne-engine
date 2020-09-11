package org.cafienne.service.api.tenant

import org.cafienne.service.api.projection.LastModifiedRegistration

object TenantReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Tenants")
}
