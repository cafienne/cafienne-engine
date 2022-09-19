package org.cafienne.infrastructure.config.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

/**
  * Simple trait to help reading child config settings and default values
  */
trait ChildConfigReader extends ConfigReader with LazyLogging {
  val parent: ConfigReader
  def path: String
  val exception: ConfigurationException = null
  def config: Config = {
    if (parent.config.hasPath(path)) {
      parent.config.getConfig(path)
    } else {
      ConfigFactory.empty()
    }
  }

  override def toString: String = s"cafienne.$fullPath"

  lazy val fullPath: String = parent match {
    case reader: ChildConfigReader => reader.path + "." + path
    case _ => path
  }
}
