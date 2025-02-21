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

package org.cafienne.service.infrastructure.configuration

import com.typesafe.config.{Config, ConfigValueType}
import org.cafienne.infrastructure.config.api.SecurityConfig
import org.cafienne.infrastructure.config.util.MandatoryConfig

import scala.jdk.CollectionConverters.ListHasAsScala

class OIDCConfiguration(val parent: SecurityConfig) extends MandatoryConfig {
  def path = "oidc"
  override lazy val config: Config = parent.config

  def getIssuer(key: String): IssuerConfiguration = {
    issuers.find(_.issuer == key).orNull
  }

  lazy val issuers: Seq[IssuerConfiguration] = {
    val list = if (config.hasPath(path)) {
      val configType = config.getValue(path).valueType()
      if (configType == ConfigValueType.LIST) {
        readConfigurations(config.getConfigList(path).asScala.toSeq)
      } else if (configType == ConfigValueType.OBJECT) {
        // This is the old style configuration on single IDP
        readConfigurations(Seq(config.getConfig(path)))
      } else {
        fail(s"Invalid type in $this configuration (found $configType, but expecting a LIST)")
      }
    } else {
      fail(s"Missing configuration property $this")
    }
    // Check that we have actual values in the list
    if (list.isEmpty) {
      fail(s"Configuration property $this cannot be left empty")
    }
    list
  }

  private def readConfigurations(configs: Seq[Config]): Seq[IssuerConfiguration] = {
    val issuers = configs.map(new IssuerConfiguration(_)).filter(_.hasMetadata)
    // Check that we have actual values in the list
    logger.warn(s"Cafienne HTTP Server is configured with ${issuers.size} identity providers: ${issuers.map(_.issuer).mkString("\n- ", "\n- ", "")}")
    issuers
  }
}
