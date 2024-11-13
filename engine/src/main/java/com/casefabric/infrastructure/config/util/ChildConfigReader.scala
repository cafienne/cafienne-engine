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
  * Simple trait to help reading child config settings and default values
  */
trait ChildConfigReader extends ConfigReader with LazyLogging {
  val parent: ConfigReader
  def path: String
  val exception: ConfigurationException = null
  def config: Config = {
    if (path.isBlank) {
      parent.config
    } else if (parent.config.hasPath(path)) {
      parent.config.getConfig(path)
    } else {
      ConfigFactory.empty()
    }
  }

  override def toString: String = s"casefabric.$fullPath"

  lazy val fullPath: String = parent match {
    case reader: ChildConfigReader => reader.path + "." + path
    case _ => path
  }
}
