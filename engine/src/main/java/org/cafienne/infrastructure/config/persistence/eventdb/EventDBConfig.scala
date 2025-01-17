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

package org.cafienne.infrastructure.config.persistence.eventdb

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.persistence.PersistenceConfig
import org.cafienne.infrastructure.config.util.ChildConfigReader

class EventDBConfig(val parent: PersistenceConfig, val systemConfig: Config) extends ChildConfigReader {
  def path = "event-db"

  lazy val journalKey: String = systemConfig.getString("pekko.persistence.journal.plugin")

  /**
    * Returns the configuration settings of the journal database.
    * This can be used to create a Slick database connection
    */
  lazy val journal: Config = {
    systemConfig.getConfig(journalKey)
  }

  lazy val jdbcConfig: JDBCConfig = new JDBCConfig(parent, systemConfig, journal)

  lazy val isJDBC: Boolean = journalKey.contains("jdbc")
  lazy val isCassandra: Boolean = journalKey.contains("cassandra")
  lazy val isLevelDB: Boolean = journalKey.contains("level")
  lazy val isInMemory: Boolean = journalKey.contains("memory")
}
