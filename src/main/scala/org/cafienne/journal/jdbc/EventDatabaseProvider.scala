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

package org.cafienne.journal.jdbc

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.jdbc.db.{DefaultSlickDatabaseProvider, SlickDatabase}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import slick.jdbc._

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

class EventDatabaseProvider(system: ActorSystem) extends DefaultSlickDatabaseProvider(system) {
  private var _db: Option[SlickDatabase] = None

  override def database(config: Config): SlickDatabase = this.synchronized {
    _db.fold({
        val db = super.database(config)
        FlywayEventDB.validateSchema(db.database, db.profile)
        _db = Some(db)
        db
      })(db => db)
  }
}

object FlywayEventDB extends LazyLogging {
  def validateSchema(db: JdbcBackend.Database, profile: JdbcProfile): MigrateResult = {
    val dataSource: DataSource = new DatabaseDataSource(db)

    val dbScriptsLocation = {
      val folder: String = profile match {
        case _: PostgresProfile => "postgres"
        case _: SQLServerProfile => "sqlserver"
        case _: H2Profile => "h2"
        //        case _: HsqldbProfile => "hsql" // not yet supported
        case _ => throw new IllegalArgumentException(s"Cannot start EventDatabase provider for unsupported JDBC profile of type ${profile.getClass.getName}")
      }
      logger.info("Running event database migrations with scripts " + folder)
      folder
    }

    val flyway = Flyway
      .configure()
      .mixed(true)
      .dataSource(dataSource)
      .locations("classpath:db/events/" + dbScriptsLocation)
      .load()
    flyway.migrate()
  }
}

private class DatabaseDataSource(db: JdbcBackend.Database) extends DataSource {
  private val conn = db.createSession().conn

  override def getConnection: Connection = conn

  override def getConnection(username: String, password: String): Connection = conn

  override def unwrap[T](iface: Class[T]): T = conn.unwrap(iface)

  override def isWrapperFor(iface: Class[_]): Boolean = conn.isWrapperFor(iface)

  override def getLoginTimeout: Int = ???

  override def setLoginTimeout(seconds: Int): Unit = ???

  override def getParentLogger: Logger = ???

  override def getLogWriter: PrintWriter = ???

  override def setLogWriter(out: PrintWriter): Unit = ???
}