package org.cafienne.persistence.eventdb

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.config.persistence.eventdb.Profile
import org.cafienne.persistence.eventdb.schema.EventDBSchema
import org.cafienne.persistence.eventdb.schema.h2.H2EventDBSchema
import org.cafienne.persistence.eventdb.schema.hsqldb.HSQLDBEventDBSchema
import org.cafienne.persistence.eventdb.schema.postgres.PostgresEventDBSchema
import org.cafienne.persistence.eventdb.schema.sqlserver.SQLServerEventDBSchema
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.flywaydb.core.api.resolver.MigrationResolver

object EventDB extends LazyLogging {
  def initializeDatabaseSchema(): MigrateResult = {
    val eventDB = Cafienne.config.persistence.eventDB
    if (!eventDB.isJDBC) {
      return null
    }

    val schema: EventDBSchema = eventDB.jdbcConfig.profile match {
      case Profile.Postgres => PostgresEventDBSchema
      case Profile.SQLServer => SQLServerEventDBSchema
      case Profile.H2 =>  H2EventDBSchema
      case Profile.HSQLDB => HSQLDBEventDBSchema
      case other => throw new IllegalArgumentException(s"Type of profile $other is not supported")
    }

    logger.info(s"Running event database migrations with schema ${schema.getClass.getSimpleName}")

    // Create configuration
    val flywayConfiguration = Flyway
      .configure()
      .dataSource(eventDB.jdbcConfig.url, eventDB.jdbcConfig.user, eventDB.jdbcConfig.password)
      .baselineOnMigrate(true)
      .baselineVersion("0.0.0")
      .baselineDescription("CaseFabric EventDB")
      .table(eventDB.schemaHistoryTable)
      .resolvers((_: MigrationResolver.Context) => schema.migrationScripts())

    // Create a connection and run migration
    val flyway = flywayConfiguration.load()
    flyway.migrate()
  }
}
