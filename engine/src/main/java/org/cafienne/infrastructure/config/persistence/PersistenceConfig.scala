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

package org.cafienne.infrastructure.config.persistence

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.CafienneConfig
import org.cafienne.infrastructure.config.persistence.eventdb.EventDBConfig
import org.cafienne.infrastructure.config.persistence.querydb.QueryDBConfig
import org.cafienne.infrastructure.config.util.ChildConfigReader


class PersistenceConfig(val parent: CafienneConfig, val systemConfig: Config) extends ChildConfigReader {
  def path = "persistence"

  lazy val journalKey: String = systemConfig.getString("pekko.persistence.journal.plugin")

  lazy val initializeDatabaseSchemas: Boolean = readBoolean("initialize-database-schema", default = true)

  /**
    * Returns configuration options for the QueryDB
    */
  lazy val queryDB: QueryDBConfig = new QueryDBConfig(this)
  lazy val eventDB: EventDBConfig = new EventDBConfig(this, systemConfig)

  lazy val readJournal: String = findReadJournalSetting()

  private def findReadJournalSetting(): String = {

    import io.github.alstanchev.pekko.persistence.inmemory.query.scaladsl.InMemoryReadJournal
    import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
    import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
    import org.apache.pekko.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal

    logger.warn("Missing conf 'cafienne.read-journal'. Trying to determine read journal settings by guessing based on the name of the journal plugin \"" + journalKey + "\"")
    if (eventDB.isJDBC) {
      return JdbcReadJournal.Identifier
    } else if (eventDB.isCassandra) {
      return CassandraReadJournal.Identifier
    } else if (eventDB.isLevelDB) {
      logger.warn("Found Level DB based configurations. This has proven to be unreliable. Do not use it in Production systems.")
      return LeveldbReadJournal.Identifier
    } else if (eventDB.isInMemory) {
      return InMemoryReadJournal.Identifier
    }
    throw new RuntimeException(s"Cannot find read journal for $journalKey, please use Cassandra or JDBC read journal settings")
  }
}
