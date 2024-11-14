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

import com.typesafe.config.{Config, ConfigValue, ConfigValueFactory}
import com.typesafe.scalalogging.LazyLogging

trait ConfigMigrator extends LazyLogging {

  def run(config: Config): Config = {
    logger.warn(s"Running an empty migrator ${this.getClass.getName}")
    config
  }

  /**
    * Print a big warning message, hopefully drawing attention :)
    */
  def printWarning(msg: String): Unit = {
    val extendedMessage = s"\tWARNING - $msg\t"
    val longestLine = extendedMessage.split("\n").map(_.length).max + 8 // Plus 8 for 2 tabs
    val manyHashes = List.fill(longestLine)('#').mkString
    // Print
    // - 2 blank lines
    // - many hashes
    // - 2 blank lines
    // - the actual message, preceded with "   WARNING - "
    // - 2 blank lines
    // - many hashes
    // - 2 blank lines
    logger.warn(s"\n\n$manyHashes\n\n$extendedMessage\n\n$manyHashes\n\n")
  }

  private def getLocationDescription(value: ConfigValue): String = {
    val origin = value.origin()
    s"${origin.url()}, line ${origin.lineNumber()}"
  }

  def quoted(path: String, key: String): String = s"""$path.$key"""
  def quotedKeyPath(path: String, key: String): String = quoted(path, s""""$key"""")

  def migrateConfigurationValue(config: Config, path: String, key: String, oldValue: AnyRef, newValue: AnyRef, showWarningOnDifferentValue: Boolean = true): Config = {
    val keyPath = quoted(path, key)
    if (config.hasPath(keyPath)) {
      val configValue = config.getValue(keyPath)
      val location = getLocationDescription(configValue)
      val value = configValue.unwrapped()
      if (value != newValue) {
        if (value == oldValue) {
          printWarning(s"""$location\n\tPlease change deprecated configuration property '$keyPath' to\n\n\t\t$key = "$newValue" """)
          return config.withValue(keyPath, ConfigValueFactory.fromAnyRef(newValue))
        } else if (showWarningOnDifferentValue) {
          printWarning(s"""$location\n\tConfiguration property '$keyPath' may have the wrong value; consider changing it to \n\n\t\t$key = "$newValue" """)
        }
      }
    }
    // Return the existing config
    config
  }

  def dropConfigurationValue(config: Config, path: String, key: String, oldValue: AnyRef, newValue: AnyRef): Config = {
    val keyPath = quoted(path, key)
    if (config.hasPath(keyPath)) {
      val configValue = config.getValue(keyPath)
      val location = getLocationDescription(configValue)
      val value = configValue.unwrapped()
      if (value == oldValue || value == newValue) {
        printWarning(s"""$location\n\tFound deprecated configuration property, please drop the line.\n\n\t\t$key = $value""")
      }
    }
    // Return the existing config
    config
  }

  /**
    * This migrates the old key to the new key.
    * Note that the key is string escaped inside the method and then gets appended to the path
    *
    * @param config
    * @param path
    * @param oldKey
    * @param newKey
    * @return
    */
  def migrateConfigurationProperty(config: Config, path: String, oldKey: String, newKey: String): Config = {
    val oldPath = quotedKeyPath(path, oldKey)
    val newPath = quotedKeyPath(path, newKey)

    if (!config.hasPath(newPath)) {
      if (config.hasPath(oldPath)) {
        val location = getLocationDescription(config.getValue(oldPath))
        val value = config.getAnyRef(oldPath)
        printWarning(s"""$location\n\tPlease change deprecated configuration property '$oldKey' to\n\n\t\t$newKey = $value""")

        // Replace the old property
        return config.withoutPath(oldPath).withValue(newPath, ConfigValueFactory.fromAnyRef(value))
      } else {
        printWarning(s"""Configuration property '$path' might be missing """)
      }
    }
    // Return the existing config
    config
  }

  def dropConfigurationProperty(config: Config, path: String, key: String): Config = {
    val propertyPath = quotedKeyPath(path, key)

    if (config.hasPath(propertyPath)) {
      val value = config.getValue(propertyPath)
      val location = getLocationDescription(value)
      printWarning(s"""$location\n\tFound deprecated configuration property, please drop the line.\n\n\t\t$key = ${value.unwrapped}""")
    }
    // Return the existing config
    config
  }

}
