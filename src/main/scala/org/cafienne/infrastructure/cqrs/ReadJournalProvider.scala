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

package org.cafienne.infrastructure.cqrs

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne

/**
  * Provides all query types of ReadJournal (eventsByTag, eventsById, etc.)
  */
trait ReadJournalProvider extends LazyLogging {
  def system: ActorSystem
  implicit def actorSystem = system

  lazy val configuredJournal: String = system.settings.config.getString("akka.persistence.journal.plugin")
  lazy val readJournalSetting: String = findReadJournalSetting()

  /**
    * Provides the requested journal
    *
    * @return
    */
  def journal(): ReadJournal with CurrentPersistenceIdsQuery with EventsByTagQuery with CurrentEventsByTagQuery with EventsByPersistenceIdQuery with CurrentEventsByPersistenceIdQuery = {
    PersistenceQuery(system).readJournalFor[ReadJournal with CurrentPersistenceIdsQuery with EventsByTagQuery with CurrentEventsByTagQuery with EventsByPersistenceIdQuery with CurrentEventsByPersistenceIdQuery](readJournalSetting)
  }

  private def findReadJournalSetting(): String = {

    val explicitReadJournal = Cafienne.config.readJournal
    if (explicitReadJournal.nonEmpty) {
      return explicitReadJournal
    }

    import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
    import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
    import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal

    logger.warn("Missing conf 'cafienne.read-journal'. Trying to determine read journal settings by guessing based on the name of the journal plugin \"" + configuredJournal + "\"")
    if (configuredJournal.contains("jdbc")) {
      return JdbcReadJournal.Identifier
    } else if (configuredJournal.contains("cassandra")) {
      return CassandraReadJournal.Identifier
    } else if (configuredJournal.contains("level")) {
      logger.warn("Found Level DB based configurations. This has proven to be unreliable. Do not use it in Production systems.")
      return LeveldbReadJournal.Identifier
    } else if (configuredJournal.contains("memory")) {
      return "inmemory-read-journal"
    }
    throw new RuntimeException(s"Cannot find read journal for $configuredJournal, please use Cassandra or JDBC read journal settings")
  }
}

