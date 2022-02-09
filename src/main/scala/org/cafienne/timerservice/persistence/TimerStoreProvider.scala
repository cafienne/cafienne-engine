package org.cafienne.timerservice.persistence

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import org.cafienne.infrastructure.cqrs.ReadJournalProvider
import org.cafienne.timerservice.persistence.cassandra.CassandraTimerStore
import org.cafienne.timerservice.persistence.inmemory.InMemoryStore
import org.cafienne.timerservice.persistence.jdbc.JDBCTimerStore

/**
  * TimerStoreProvider can return a storage object to persist timer events
  * @param system
  */
class TimerStoreProvider(implicit override val system: ActorSystem) extends ReadJournalProvider {

  val store: TimerStore = {
    journal() match {
      case c: CassandraReadJournal => new CassandraTimerStore(c)
      case _: JdbcReadJournal => new JDBCTimerStore()
      case _ => new InMemoryStore() // By default return in memory map
    }
  }
}
