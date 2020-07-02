package org.cafienne.service.api.tenant

import org.cafienne.service.api.projection.LastModifiedRegistration
import org.cafienne.tenant.akka.event.TenantModified

object TenantReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration("Tenants")

  def inform(tenantModified: TenantModified) = {
    lastModifiedRegistration.handle(tenantModified)
  }
}
