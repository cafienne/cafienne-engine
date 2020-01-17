package org.cafienne.service.db.events

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger

import akka.actor.ActorSystem
import akka.persistence.jdbc.util.{DefaultSlickDatabaseProvider, SlickDatabase, SlickDatabaseProvider}
import com.typesafe.config.Config
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import slick.jdbc.{JdbcBackend, JdbcProfile}

class EventDatabaseProvider(system: ActorSystem) extends DefaultSlickDatabaseProvider(system) {

  private def createOrMigrate(db: JdbcBackend.Database): Unit = {
    val flyway = Flyway
      .configure()
      .dataSource(new DatabaseDataSource(db))
      .locations("classpath:db/events/postgres")
      .load()
    flyway.migrate()
  }

  override def database(config: Config): SlickDatabase = {
    val db = super.database(config)
    createOrMigrate(db.database)
    db
  }
}

private class DatabaseDataSource(db: JdbcBackend.Database) extends DataSource {
  private val conn = db.createSession().conn

  override def getConnection: Connection = conn
  override def getConnection(username: String, password: String): Connection = conn
  override def unwrap[T](iface: Class[T]): T = conn.unwrap(iface)
  override def isWrapperFor(iface: Class[_]): Boolean = conn.isWrapperFor(iface)

  override def setLogWriter(out: PrintWriter): Unit = ???
  override def getLoginTimeout: Int = ???
  override def setLoginTimeout(seconds: Int): Unit = ???
  override def getParentLogger: Logger = ???
  override def getLogWriter: PrintWriter = ???
}