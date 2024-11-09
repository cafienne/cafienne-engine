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

package org.cafienne.system.health

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.json.ValueMap

import java.time.Instant

class HealthMeasurePoint(val key: String, val isCritical: Boolean) extends LazyLogging {
  private var healthy = true
  private var description = key + " is healthy "
  private var changed: Instant = _

  def unhealthy(): Boolean = !healthy

  /**
    * Report that the measure point is healthy.
    */
  def isOK(): Unit = {
    if (! healthy) {
      logger.error("{} is healthy health again", key)
      healthy = true
      description = key + " is healthy "
      changed = Instant.now
    }
  }

  /**
    * Report that the measure point has failed
    * @param throwable
    */
  def hasFailed(throwable: Throwable): Unit = {
    logger.error(s"$key reported bad health", throwable)

    healthy = false
    description = "Failure: " + throwable.getLocalizedMessage
    changed = Instant.now
  }

  def status: String = {
    if (healthy) "OK"
    else "NOK"
  }

  def asJSON(): ValueMap = {
    val json = new ValueMap("Status", status, "Description", description)
    if (changed != null) {
      json.plus("changed-at", changed)
    }
    json
  }
}
