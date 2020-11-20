package org.cafienne.akka.actor.config.util

import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

/**
  * Some utility methods to easily read properties from a scala Config object with e.g. default values
  */
trait ConfigReader {
  val config: Config

  def fail(msg: String) = {
    throw ConfigurationException(msg)
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

  def readBoolean(path: String, default: Boolean): Boolean = {
    if (config != null && config.hasPath(path)) {
      config.getBoolean(path)
    } else {
      default
    }
  }

  def readDuration(name: String, default: FiniteDuration): FiniteDuration = {
    if (config != null && config.hasPath(name)) {
      FiniteDuration(config.getDuration(name).toSeconds, TimeUnit.SECONDS)
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

  def readConfig(path: String, defaultValue: Config = ConfigFactory.empty()): Config = {
    if (config.hasPath(path)) {
      config.getConfig(path)
    } else {
      defaultValue
    }
  }

  def getConfigReader(path: String, defaultValue: Config = ConfigFactory.empty()): ConfigReader = {
    ConfigReader(readConfig(path, defaultValue))
  }

  def readConfigList(path: String, defaultValue: Seq[Config] = Seq()): Seq[ConfigReader] = {
    val list = if (config.hasPath(path)) {
      config.getObjectList(path).asScala.map(configObject => configObject.toConfig)
    } else {
      defaultValue
    }
    list.map(c => ConfigReader(c)).toSeq
  }

  def requires(errorPrefixMessage: String, paths: String*) = {
    val missingPaths = paths.filter(path => ! config.hasPath(path)).map(p => s"'$p'")
    if (missingPaths.nonEmpty) fail(errorPrefixMessage +" misses config properties " + missingPaths.mkString(", "))
  }
}

object ConfigReader {
  def apply(baseConfig: Config) = new ConfigReader {
    override val config: Config = baseConfig
  }
}
