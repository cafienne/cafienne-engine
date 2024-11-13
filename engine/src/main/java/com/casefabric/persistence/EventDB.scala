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
