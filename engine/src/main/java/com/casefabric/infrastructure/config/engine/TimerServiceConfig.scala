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

package com.casefabric.infrastructure.config.engine

import com.casefabric.infrastructure.config.util.ChildConfigReader

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class TimerServiceConfig(val parent: EngineConfig) extends ChildConfigReader {
  def path = "timer-service"

  /**
    * Returns configuration path for the event store
    */
  lazy val store: String = {
    if (config.hasPath("store")) {
      readString("store")
    } else {
      val defaultValue = "pekko-persistence-jdbc.shared-databases.slick"
      logger.warn("Event store configuration is missing, assuming default value " + defaultValue)
      defaultValue
    }
  }

  /**
    * Returns the duration of the window ahead to set timers in the in-memory scheduler
    */
  val window: FiniteDuration = {
    readDuration("window", FiniteDuration(65, TimeUnit.MINUTES))
  }

  /**
    * Returns the cycle time to refresh the timers loaded into the in-memory scheduler.
    * Cannot be longer than the duration given for the 'window'
    */
  val interval: FiniteDuration = {
    val interval = readDuration("interval", FiniteDuration(1, TimeUnit.HOURS))
    if (interval >= window) {
      fail(s"Timer service refresh interval (configured to $interval) must be shorter than the window ahead (which is configured to $window)")
    }
    interval
  }
}