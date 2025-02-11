/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.infrastructure.config

import com.typesafe.config.{ConfigValue, ConfigValueFactory, ConfigValueType}
import org.cafienne.actormodel.identity.{PlatformOwner, UserIdentity}
import org.cafienne.infrastructure.config.util.MandatoryConfig

import scala.jdk.CollectionConverters.CollectionHasAsScala

class PlatformConfig(val parent: CaseSystemConfig) extends MandatoryConfig {
  def path = "platform"

  val platformOwners: Seq[PlatformOwner] = config.getStringList("owners").asScala.map(PlatformOwner(_)).toSeq
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

  def isPlatformOwner(user: UserIdentity): Boolean = {
    // TTP: platformOwners should be taken as Set and "toLowerCase" initially, and then we can do "contains" instead
    logger.debug("Checking whether user " + user.id + " is a platform owner; list of owners: " + platformOwners)
    platformOwners.exists(o => o.id.equalsIgnoreCase(user.id))
  }
}
