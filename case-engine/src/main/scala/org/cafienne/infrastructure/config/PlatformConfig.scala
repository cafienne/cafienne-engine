package org.cafienne.infrastructure.config

import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.infrastructure.config.util.MandatoryConfig

import java.util.List

class PlatformConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "platform"

  val platformOwners: List[String] = config.getStringList("owners")
  if (platformOwners.isEmpty) {
    fail("Platform owners cannot be an empty list. Check configuration property cafienne.platform.owners")
  }

  lazy val defaultTenant = {
    val configuredDefaultTenant = readString("default-tenant")
    configuredDefaultTenant
  }

  /**
    * Config property for reading a specific file with bootstrap tenant setup
    */
  lazy val bootstrapFile = readString("bootstrap-file")

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    // TTP: platformOwners should be taken as Set and "toLowerCase" initially, and then we can do "contains" instead
    logger.debug("Checking whether user " + userId + " is a platform owner; list of owners: " + platformOwners)
    platformOwners.stream().filter(o => o.equalsIgnoreCase(userId)).count() > 0
  }
}