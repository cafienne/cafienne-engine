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

import org.cafienne.cmmn.repository.DefinitionProvider
import org.cafienne.infrastructure.config.util.MandatoryConfig

class RepositoryConfig(val parent: CaseSystemConfig) extends MandatoryConfig {
  def path = "definitions"
  override val msg = "Cafienne Repository is not configured. Check for 'cafienne.definitions' settings"

  /**
    * DefinitionProvider provides an interface for loading Case Definitions
    */
  lazy val DefinitionProvider: DefinitionProvider = {
    val providerClassName = config.getString("provider")
    Class.forName(providerClassName).getDeclaredConstructor(classOf[RepositoryConfig]).newInstance(this).asInstanceOf[DefinitionProvider]
  }

  val location: String = config.getString("location")

  lazy val cacheSize: Int = {
    if (config.hasPath("cache.size")) config.getInt("cache.size")
    100
  }
}
