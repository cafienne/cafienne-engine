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

package com.casefabric.timerservice.persistence

import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import com.casefabric.infrastructure.cqrs.ReadJournalProvider
import com.casefabric.system.CaseSystem
import com.casefabric.timerservice.persistence.cassandra.CassandraTimerStore
import com.casefabric.timerservice.persistence.inmemory.InMemoryStore
import com.casefabric.timerservice.persistence.jdbc.JDBCTimerStore

/**
  * TimerStoreProvider can return a storage object to persist timer events
  * @param system
  */
class TimerStoreProvider(val caseSystem: CaseSystem) extends ReadJournalProvider {
  override val system = caseSystem.system

  val store: TimerStore = {
    journal() match {
      case c: CassandraReadJournal => new CassandraTimerStore(c)
      case _: JdbcReadJournal => new JDBCTimerStore()
      case _ => new InMemoryStore() // By default return in memory map
    }
  }
}
