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

package com.casefabric.infrastructure.config

import org.apache.pekko.util.Timeout
import com.casefabric.infrastructure.config.util.ChildConfigReader

import scala.concurrent.duration.SECONDS

class ModelActorConfig(val parent: CaseFabricConfig) extends ChildConfigReader {
  def path = "actor"

  lazy val askTimout: Timeout = {
    val default = 60
    val period = readLong("ask-timeout", default)
    logger.info("CommandRoutes wait a maximum of " + period + " seconds for a response upon their requests")
    Timeout(period, SECONDS)
  }

  lazy val idlePeriod: Long = {
    val default = 60 * 10
    val period = readLong("idle-period", default)
    logger.info("Individual Case instances will be removed from memory after they have been idle for " + period + " seconds")
    period * 1000
  }

  /**
    * Setting to indicate whether ModelActors should start in debug mode or not (by default).
    * Currently only implemented for cases. Also StartCase command has option to override the default setting
    */
  lazy val debugEnabled: Boolean = {
    // Note: for now, we can better take this from model-actor config, but we should also read the old option
    readBoolean("debug", parent.readBoolean("debug", default = false))
  }
}
