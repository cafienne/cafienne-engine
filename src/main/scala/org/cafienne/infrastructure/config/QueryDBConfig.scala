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

package org.cafienne.infrastructure.config

import org.apache.pekko.stream.RestartSettings
import org.cafienne.infrastructure.config.persistence.PersistenceConfig
import org.cafienne.infrastructure.config.util.{ChildConfigReader, MandatoryConfig}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration


class QueryDBConfig(val parent: PersistenceConfig) extends MandatoryConfig {
  def path = "query-db"
  override val msg = "Cafienne Query Database is not configured. Check local.conf for 'cafienne.query-db' settings"

  lazy val restartSettings: RestartSettings = new RestartConfig(this).settings
  lazy val debug: Boolean = readBoolean("debug", default = false)
  lazy val readJournal: String = {
    val foundJournal = readString("read-journal")
    logger.warn(s"Obtaining read-journal settings from 'cafienne.querydb.read-journal' = $foundJournal is deprecated; please place these settings in 'cafienne.read-journal' instead")
    foundJournal
  }

}

class RestartConfig(val parent: QueryDBConfig) extends ChildConfigReader {
  def path = "restart-stream"

  lazy val minBackoff: FiniteDuration = readDuration("min-back-off", FiniteDuration(500, TimeUnit.MILLISECONDS))
  lazy val maxBackoff: FiniteDuration = readDuration("max-back-off", FiniteDuration(30, TimeUnit.SECONDS))
  lazy val randomFactor: Double = readNumber("random-factor", 0.2).doubleValue()
  lazy val maxRestarts: Int = readInt("max-restarts", 20)
  lazy val maxRestartsWithin: FiniteDuration = readDuration("max-restarts-within", FiniteDuration(5, TimeUnit.MINUTES))

  lazy val settings: RestartSettings = RestartSettings(minBackoff, maxBackoff, randomFactor).withMaxRestarts(maxRestarts, maxRestartsWithin)
}
