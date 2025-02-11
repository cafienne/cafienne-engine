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

package org.cafienne.timerservice.persistence

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import org.cafienne.infrastructure.cqrs.ReadJournalProvider
import org.cafienne.system.CaseSystem
import org.cafienne.timerservice.persistence.cassandra.CassandraTimerStore
import org.cafienne.timerservice.persistence.inmemory.InMemoryStore
import org.cafienne.timerservice.persistence.jdbc.JDBCTimerStore
import slick.basic.DatabaseConfig

/**
 * TimerStoreProvider can return a storage object to persist timer events
 */
class TimerStoreProvider(val caseSystem: CaseSystem) extends ReadJournalProvider {
  override val system: ActorSystem = caseSystem.system

  val store: TimerStore = {
    journal() match {
      case c: CassandraReadJournal => new CassandraTimerStore(c)
      case _: JdbcReadJournal =>
        val timerStoreConfigKey: String = caseSystem.config.engine.timerService.store
        val timerStoreConfig = caseSystem.config.systemConfig.config.getConfig(timerStoreConfigKey)
//        val msg = s"""journalConfig = ${journalConfig.root().render(ConfigRenderOptions.concise().setFormatted(true))}"""
//        logger.info("Using config to start jdbc ts : " + msg)
        new JDBCTimerStore(DatabaseConfig.forConfig("", timerStoreConfig))
      case _ => new InMemoryStore() // By default return in memory map
    }
  }
}
