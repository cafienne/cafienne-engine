package org.cafienne.infrastructure.config.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

/**
  * Some utility methods to easily read properties from a scala Config object with e.g. default values
  */
trait ConfigReader extends LazyLogging {
  val config: Config

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
