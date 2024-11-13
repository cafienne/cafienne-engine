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

package com.casefabric.infrastructure.config.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

/**
  * Static helper to load config settings for this JVM
  * Also migrates deprecated property values if found
  */
object SystemConfig extends ConfigMigrator with LazyLogging {

  private var config: Config = ConfigFactory.defaultReference()
  private var loaded: Boolean = false

  def migrate(migrators: ConfigMigrator*): SystemConfig.type = {
    if (!loaded) {
      throw new Error("Cannot migrate the configuration because it first must be loaded through the load() method")
    }
    migrators.foreach(migrator => {
      logger.info("Running ConfigMigrator " + migrator.getClass.getName)
      config = migrator.run(config)
    })
    this
  }

  def getConfig: Config = config

  def load(fileName: String = ""): SystemConfig.type = {
    if (loaded) {
      println("loading config again?")
    }
    if (fileName.isBlank) {
      config = ConfigFactory.load().withFallback(config)
    } else {
      config = ConfigFactory.load(fileName).withFallback(config)
    }
    loaded = true
    this
  }
}
