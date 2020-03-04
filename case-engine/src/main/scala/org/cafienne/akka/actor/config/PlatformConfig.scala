package org.cafienne.akka.actor.config

import java.util

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.TenantUser

class PlatformConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "platform"
  override val exception: Throwable = new IllegalArgumentException("Check configuration property 'cafienne.platform'. This must be available")

  val platformOwners: util.List[String] = config.getStringList("owners")
  if (platformOwners.isEmpty) {
    throw new IllegalArgumentException("Platform owners cannot be an empty list. Check configuration property cafienne.platform.owners")
  }

  lazy val defaultTenant = {
    val configuredDefaultTenant = readString("default-tenant", "")
    configuredDefaultTenant
  }

  /**
    * Config property for reading a specific file with bootstrap tenant setup
    */
  lazy val bootstrapFile = readString("bootstrap-file", "")

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    // TTP: platformOwners should be taken as Set and "toLowerCase" initially, and then we can do "contains" instead
    logger.debug("Checking whether user " + userId + " is a platform owner; list of owners: " + platformOwners)
    platformOwners.stream().filter(o => o.equalsIgnoreCase(userId)).count() > 0
  }
}