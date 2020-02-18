package org.cafienne.akka.actor.config

import java.util

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.command.exception.MissingTenantException
import org.cafienne.akka.actor.identity.TenantUser

class PlatformConfig(val parentConfig: Config) extends LazyLogging {

  val config = {
    if (parentConfig.hasPath("platform")) {
      parentConfig.getConfig("platform")
    } else {
      throw new IllegalArgumentException("Check configuration property 'cafienne.platform'. This must be available")
    }
  }

  val platformOwners: util.List[String] = config.getStringList("owners")
  if (platformOwners.isEmpty) {
    throw new IllegalArgumentException("Platform owners cannot be an empty list. Check configuration property cafienne.platform.owners")
  }

  lazy val defaultTenant = {
    val configuredDefaultTenant = if (config.hasPath("default-tenant")) {
      config.getString("default-tenant")
    } else {
      ""
    }
    if (configuredDefaultTenant.isEmpty) {
      throw new MissingTenantException("Tenant property must have a value")
    }
    configuredDefaultTenant
  }

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    // TTP: platformOwners should be taken as Set and "toLowerCase" initially, and then we can do "contains" instead
    logger.debug("Checking whether user " + userId + " is a platform owner; list of owners: " + platformOwners)
    platformOwners.stream().filter(o => o.equalsIgnoreCase(userId)).count() > 0
  }

}