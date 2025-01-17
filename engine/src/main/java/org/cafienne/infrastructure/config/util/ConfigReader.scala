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

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

/**
  * Some utility methods to easily read properties from a scala Config object with e.g. default values
  */
trait ConfigReader extends LazyLogging {
  def config: Config

  /**
   * Print the config in a concise manner (i.e., what has been actually found in the configuration, instead of also default values)
   */
  def printConfig(): Unit = {
    println(s"""$toString = ${config.root().render(ConfigRenderOptions.concise().setFormatted(true))}""")
  }

  def warn(msg: String): Unit = {
    logger.warn(msg)
  }

  def readNumber(path: String, default: Number): Number = {
    if (config != null && config.hasPath(path)) {
      config.getNumber(path)
    } else {
      default
    }
  }

  def readLong(path: String, default: Long): Long = {
    if (config != null && config.hasPath(path)) {
      config.getLong(path)
    } else {
      default
    }
  }

  def readInt(path: String, default: Int): Int = {
    if (config != null && config.hasPath(path)) {
      config.getInt(path)
    } else {
      default
    }
  }

  def readString(path: String, default: String = ""): String = {
    if (config != null && config.hasPath(path)) {
      config.getString(path)
    } else {
      default
    }
  }

  def readEnum[T <: AnyRef](path: String, enumClass: Class[T], default: T = null): T = {
    if (config != null && config.hasPath(path)) {
      val string = config.getString(path)
      if (string == null) { // No value found, just return the default;
        return default
      }
      // Try to find valueOf method and use that to instantiate the enum.
      try {
        val m = enumClass.getMethod("valueOf", classOf[String])
        m.invoke(enumClass, string).asInstanceOf[T]
      } catch {
        case e@(_: IllegalAccessException | _: IllegalArgumentException | _: InvocationTargetException | _: NoSuchMethodException | _: SecurityException) =>
          throw new IllegalArgumentException(e.fillInStackTrace)
      }
    } else {
      default
    }
  }

  def readBoolean(path: String, default: Boolean): Boolean = {
    if (config != null && config.hasPath(path)) {
      config.getBoolean(path)
    } else {
      default
    }
  }

  def readDuration(name: String, default: FiniteDuration): FiniteDuration = {
    if (config != null && config.hasPath(name)) {
      val duration = FiniteDuration(config.getDuration(name).toSeconds, TimeUnit.SECONDS).toCoarsest
      if (duration.toMillis < 0) {
        fail(s"Duration cannot be negative (found $this.$name = ${config.getValue(name).unwrapped()})")
      }
      duration
    } else {
      default
    }
  }

  def readStringList(path: String, defaultValue: Seq[String] = Seq()): Seq[String] = {
    if (config.hasPath(path)) {
      config.getStringList(path).asScala.toSeq
    } else {
      defaultValue
    }
  }

  def getConfigReader(path: String, defaultValue: Config = ConfigFactory.empty()): ConfigReader = {
    ConfigReader(readConfig(path, defaultValue))
  }

  def readConfig(path: String, defaultValue: Config = ConfigFactory.empty()): Config = {
    if (config.hasPath(path)) {
      config.getConfig(path)
    } else {
      defaultValue
    }
  }

  def readConfigList(path: String, defaultValue: Seq[Config] = Seq()): Seq[ConfigReader] = {
    val list = if (config.hasPath(path)) {
      config.getObjectList(path).asScala.map(configObject => configObject.toConfig)
    } else {
      defaultValue
    }
    list.map(c => ConfigReader(c)).toSeq
  }

  def requires(errorPrefixMessage: String, paths: String*): Unit = {
    val missingPaths = paths.filter(path => !config.hasPath(path)).map(p => s"'$p'")
    if (missingPaths.nonEmpty) fail(errorPrefixMessage + " misses config properties " + missingPaths.mkString(", "))
  }

  def fail(msg: String): Nothing = {
    throw ConfigurationException(msg)
  }
}

object ConfigReader {
  def apply(baseConfig: Config): ConfigReader = new ConfigReader {
    override val config: Config = baseConfig
  }
}
