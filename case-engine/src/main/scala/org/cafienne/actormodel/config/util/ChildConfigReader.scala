package org.cafienne.actormodel.config.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

/**
  * Simple trait to help reading child config settings and default values
  */
trait ChildConfigReader extends ConfigReader with LazyLogging {
  val parent: ConfigReader
  val path: String
  val exception: ConfigurationException = null
  lazy val config: Config = {
    parent.config.hasPath(path) match {
      case true => parent.config.getConfig(path)
      case false => ConfigFactory.empty()
    }
  }

  lazy val fullPath: String = parent.isInstanceOf[ChildConfigReader] match {
    case true => parent.asInstanceOf[ChildConfigReader].path + "." + path
    case false => path
  }
}
