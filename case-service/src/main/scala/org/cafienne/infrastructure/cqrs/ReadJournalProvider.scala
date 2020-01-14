package org.cafienne.infrastructure.cqrs

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.scaladsl._
import com.typesafe.scalalogging.LazyLogging

/**
  * Provides a readJournal that has the eventsByTag available that's used for
  * creation of the query models of the system.
  */
trait ReadJournalProvider extends LazyLogging with ActorSystemProvider {
  val configuredJournal = system.settings.config.getString("akka.persistence.journal.plugin")

  implicit def system: ActorSystem

  def readJournal() : EventsByTagQuery = {
    logger.debug("found configured journal " + configuredJournal)
    if (configuredJournal.endsWith("leveldb")) {
      logger.debug("configuring read journal for leveldb")
      return PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
    }
    if (configuredJournal.endsWith("cassandra-journal")){
      return PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
    }
    if (configuredJournal.endsWith("inmemory-journal")) {
      return PersistenceQuery(system).readJournalFor("inmemory-read-journal")
        .asInstanceOf[ReadJournal with CurrentPersistenceIdsQuery with CurrentEventsByPersistenceIdQuery with CurrentEventsByTagQuery with EventsByPersistenceIdQuery with EventsByTagQuery]
    }
    if (configuredJournal.endsWith("jdbc-journal")) {
      return PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
    }

    logger.debug("and throw the exception anyway")
    throw new RuntimeException(s"Unsupported read journal $configuredJournal, please switch to cassandra or JDBC for production")
  }

  def instanceJournal() : CurrentEventsByPersistenceIdQuery = {
    logger.debug("found configured journal " + configuredJournal)
    if (configuredJournal.endsWith("leveldb")) {
      logger.debug("configuring read journal for leveldb")
      return PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
    }
    if (configuredJournal.endsWith("cassandra-journal")){
      return PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
    }
    if (configuredJournal.endsWith("inmemory-journal")) {
      return PersistenceQuery(system).readJournalFor("inmemory-read-journal")
        .asInstanceOf[ReadJournal with CurrentPersistenceIdsQuery with CurrentEventsByPersistenceIdQuery with CurrentEventsByTagQuery with EventsByPersistenceIdQuery with EventsByTagQuery]
    }
    if (configuredJournal.endsWith("jdbc-journal")) {
      return PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
    }

    logger.debug("and throw the exception anyway")
    throw new RuntimeException(s"Unsupported read journal $configuredJournal, please switch to cassandra or JDBC for production")
  }
}
