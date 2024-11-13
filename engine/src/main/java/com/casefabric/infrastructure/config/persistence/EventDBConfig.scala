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

package com.casefabric.infrastructure.config.persistence

import com.typesafe.config.Config
import com.casefabric.infrastructure.config.util.ChildConfigReader


class EventDBConfig(val parent: PersistenceConfig, val systemConfig: Config) extends ChildConfigReader {
  def path = ""

  lazy val journalKey: String = systemConfig.getString("pekko.persistence.journal.plugin")


  lazy val readJournal: String = findReadJournalSetting()

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


  private def findReadJournalSetting(): String = {

    import io.github.alstanchev.pekko.persistence.inmemory.query.scaladsl.InMemoryReadJournal
    import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
    import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
    import org.apache.pekko.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal

    logger.warn("Missing conf 'casefabric.read-journal'. Trying to determine read journal settings by guessing based on the name of the journal plugin \"" + journalKey + "\"")
    if (isJDBC) {
      return JdbcReadJournal.Identifier
    } else if (isCassandra) {
      return CassandraReadJournal.Identifier
    } else if (isLevelDB) {
      logger.warn("Found Level DB based configurations. This has proven to be unreliable. Do not use it in Production systems.")
      return LeveldbReadJournal.Identifier
    } else if (isInMemory) {
      return InMemoryReadJournal.Identifier
    }
    throw new RuntimeException(s"Cannot find read journal for $journalKey, please use Cassandra or JDBC read journal settings")
  }
}
