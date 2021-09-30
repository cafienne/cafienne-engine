package org.cafienne.infrastructure.config

import com.typesafe.config.{ConfigValue, ConfigValueFactory, ConfigValueType}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.infrastructure.config.util.MandatoryConfig

import scala.jdk.CollectionConverters.CollectionHasAsScala

class PlatformConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "platform"

  val platformOwners: java.util.List[String] = config.getStringList("owners")
  if (platformOwners.isEmpty) {
    fail("Platform owners cannot be an empty list. Check configuration property cafienne.platform.owners")
  }

  lazy val defaultTenant: String = {
    val configuredDefaultTenant = readString("default-tenant")
    configuredDefaultTenant
  }

  /**
    * Config property for reading a specific file with bootstrap tenant setup
    */
  lazy val bootstrapFile: Seq[String] = {
    val propertyName = "bootstrap-tenants"
    val deprecatedPropertyName = "bootstrap-file"

    def readProperty(): ConfigValue = {
      if (config.hasPath(propertyName)) {
        config.getValue(propertyName)
      } else if (config.hasPath(deprecatedPropertyName)) {
        warn(s"Property $deprecatedPropertyName is deprecated. Please use $propertyName instead.")
        config.getValue(deprecatedPropertyName)
      } else {
        ConfigValueFactory.fromAnyRef(null)
      }
    }

    val value = readProperty()
    if (value.valueType() == ConfigValueType.LIST) {
      value.unwrapped().asInstanceOf[java.util.List[String]].asScala.toSeq
    } else if (value.valueType() == ConfigValueType.STRING) {
      Seq(value.unwrapped().asInstanceOf[String])
    } else if (value.valueType() == ConfigValueType.NULL) {
      Seq()
    } else {
      fail(s"Config setting '$propertyName' must hold either a string or a list of strings; found a ${value.valueType()}")
    }
  }

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    // TTP: platformOwners should be taken as Set and "toLowerCase" initially, and then we can do "contains" instead
    logger.debug("Checking whether user " + userId + " is a platform owner; list of owners: " + platformOwners)
    platformOwners.stream().filter(o => o.equalsIgnoreCase(userId)).count() > 0
  }
}