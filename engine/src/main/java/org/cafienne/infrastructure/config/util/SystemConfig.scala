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

package org.cafienne.infrastructure.config.util

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.config.CaseSystemConfig

/**
 * Static helper to load config settings for this JVM
 * Also migrates deprecated property values if found
 */
class SystemConfig(loader: => Config, migrators: ConfigMigrator*) extends ConfigMigrator with ConfigReader with LazyLogging {
  private lazy val nativeConfig = {
    var config = loader
    logger.whenDebugEnabled(logger.debug("Creating SystemConfig object based on the following underlying configuration"))
    logger.whenDebugEnabled(logger.debug(s"Configuration: ${config.root().render(ConfigRenderOptions.defaults().setFormatted(true))}"))
    migrators.foreach(migrator => {
      logger.info("Running ConfigMigrator " + migrator.getClass.getName)
      config = migrator.run(config)
    })
    logger.whenDebugEnabled(logger.debug("Resulting configuration after running migrators"))
    logger.whenDebugEnabled(logger.debug(s"Configuration: ${config.root().render(ConfigRenderOptions.concise().setFormatted(true))}"))
    config
  }

  def config: Config = nativeConfig

  lazy val cafienne: CaseSystemConfig = new CaseSystemConfig(this)

  def getConfig: Config = config

}

object SystemConfig {
  lazy val DEFAULT: SystemConfig = new SystemConfig(load())

  def load(fileName: String = "", classLoader: ClassLoader = this.getClass.getClassLoader): Config = {
    if (fileName.isBlank) {
      ConfigFactory.load(classLoader).withFallback(ConfigFactory.defaultReference())
    } else {
      ConfigFactory.load(classLoader, fileName).withFallback(ConfigFactory.defaultReference())
    }
  }
}
