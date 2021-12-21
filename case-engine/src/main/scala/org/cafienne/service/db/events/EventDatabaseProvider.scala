package org.cafienne.service.db.events

import akka.actor.ActorSystem
import akka.persistence.jdbc.db.{DefaultSlickDatabaseProvider, SlickDatabase}
import com.typesafe.config.Config
import org.flywaydb.core.Flyway
import slick.jdbc._

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

class EventDatabaseProvider(system: ActorSystem) extends DefaultSlickDatabaseProvider(system) {

  private def createOrMigrate(db: JdbcBackend.Database, profile: JdbcProfile) = {
    val dbScriptsLocation = {
      profile match {
        case _: PostgresProfile => "postgres"
        case _: SQLServerProfile => "sqlserver"
        case _: H2Profile => "h2"
//        case _: HsqldbProfile => "hsql" // not yet supported
        case _ => throw new IllegalArgumentException(s"Cannot start EventDatabase provider for unsupported JDBC profile of type ${profile.getClass.getName}")
      }
    } 

    val flyway = Flyway
      .configure()
      .dataSource(new DatabaseDataSource(db))
      .locations("classpath:db/events/" + dbScriptsLocation)
      .load()
    flyway.migrate()
  }

  override def database(config: Config): SlickDatabase = {
    val db = super.database(config)
    createOrMigrate(db.database, db.profile)
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