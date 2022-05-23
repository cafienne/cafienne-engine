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

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.config.api.{ApiConfig, OIDCConfig}
import org.cafienne.infrastructure.config.engine.EngineConfig
import org.cafienne.infrastructure.config.util.{ConfigReader, SystemConfig}

/**
  * Configuration settings of this Cafienne Case System Platform
  * @param systemConfig
  */
class CafienneConfig extends ConfigReader with LazyLogging {
  val systemConfig: Config = SystemConfig.load()

  val path = "cafienne"
  override lazy val config: Config = {
    if (systemConfig.hasPath(path)) {
      systemConfig.getConfig(path)
    } else {
      fail("Cafienne System is not configured. Check local.conf for 'cafienne' settings")
    }
  }

  /**
    * Returns configuration options for the platform, e.g. default tenant, list of platform owners
    */
  val platform: PlatformConfig = new PlatformConfig(this)

  /**
    * Returns configuration options for the QueryDB
    */
  lazy val readJournal: String = {
    if (config.hasPath("read-journal")) {
      readString("read-journal")
    } else {
      queryDB.readJournal
    }
  }

  /**
    * Returns configuration options for the QueryDB
    */
  lazy val queryDB: QueryDBConfig = new QueryDBConfig(this)

  /**
    * Returns configuration options for Model Actors
    */
  lazy val actor: ModelActorConfig = new ModelActorConfig(this)

  /**
    * Returns configuration options for the HTTP APIs
    */
  lazy val api: ApiConfig = new ApiConfig(this)

  /**
    * Returns the Open ID Connect configuration settings of this Case System
    */
  lazy val OIDC: OIDCConfig = api.security.oidc

  /**
    * Returns configuration options for reading and writing case definitions
    */
  lazy val repository: RepositoryConfig = new RepositoryConfig(this)

  lazy val storage: StorageConfig = new StorageConfig(this)

  /**
    * Returns configuration options for the engine and it's internal services
    */
  val engine: EngineConfig = new EngineConfig(this)

  /**
    * Returns true of the debug route is open (for developers using IDE to do debugging)
    */
  val developerRouteOpen: Boolean = {
    val debugRouteOpenOption = "api.security.debug.events.open"
    val open = readBoolean(debugRouteOpenOption, default = false)
    if (open) {
      SystemConfig.printWarning("Case Service runs in developer mode (the debug route to get all events is open for anyone!)")
    }
    open
  }
}

