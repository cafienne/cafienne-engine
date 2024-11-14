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

package com.casefabric.persistence

import com.typesafe.scalalogging.LazyLogging
import com.casefabric.infrastructure.CaseFabric
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

object EventDB extends LazyLogging {
  def initializeDatabaseSchema(): MigrateResult = {
    val eventDB = CaseFabric.config.persistence.eventDB
    if (!eventDB.isJDBC) {
      return null
    }

    val jdbcConfig = eventDB.jdbcConfig
    val dbScriptsLocation = {
      if (jdbcConfig.isSQLServer) "sqlserver"
      else if (jdbcConfig.isPostgres) "postgres"
      else if (jdbcConfig.isH2) "h2"
      else throw new IllegalArgumentException(s"Cannot start EventDatabase provider for unsupported JDBC profile of type ${jdbcConfig.profile}")
    }

    logger.info("Running event database migrations with scripts " + dbScriptsLocation)

    val flyway = Flyway.configure()
    jdbcConfig.user.fold{
      flyway.dataSource(jdbcConfig.url, null, null)
    }{ user =>
      jdbcConfig.password.fold(flyway.dataSource(jdbcConfig.url, user, ""))(password => flyway.dataSource(jdbcConfig.url, user, password))
    }
    flyway
      .locations("classpath:db/events/" + dbScriptsLocation)
      //  Then create an actual connection
      .load()
      // Finally start the migration
      .migrate()
  }
}
