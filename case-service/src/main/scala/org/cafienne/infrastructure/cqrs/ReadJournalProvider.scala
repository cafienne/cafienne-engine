package org.cafienne.infrastructure.cqrs

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.CaseSystem

/**
  * Provides all query types of ReadJournal (eventsByTag, eventsById, etc.)
  */
trait ReadJournalProvider extends LazyLogging {
  val configuredJournal = system.settings.config.getString("akka.persistence.journal.plugin")
  val readJournalSetting = findReadJournalSetting()

  implicit def system: ActorSystem

  /**
    * Provides the requested journal
    *
    * @return
    */
  def journal() = {
    PersistenceQuery(system).readJournalFor[ReadJournal with EventsByTagQuery with CurrentEventsByPersistenceIdQuery](readJournalSetting)
  }

  private def findReadJournalSetting(): String = {

    val explicitReadJournal = CaseSystem.config.queryDB.readJournal
    if (!explicitReadJournal.isEmpty) {
      return explicitReadJournal
    }

    import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
    import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
    import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal

    logger.warn("Missing conf 'cafienne.query-db.read-journal'. Trying to determine read journal settings by guessing based on the name of the journal plugin \"" + configuredJournal + "\"")
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

