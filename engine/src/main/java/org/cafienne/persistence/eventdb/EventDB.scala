package org.cafienne.persistence.eventdb

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.config.persistence.PersistenceConfig
import org.cafienne.infrastructure.config.persistence.eventdb.Profile
import org.cafienne.persistence.eventdb.schema.EventDBSchema
import org.cafienne.persistence.eventdb.schema.h2.H2EventDBSchema
import org.cafienne.persistence.eventdb.schema.hsqldb.HSQLDBEventDBSchema
import org.cafienne.persistence.eventdb.schema.postgres.PostgresEventDBSchema
import org.cafienne.persistence.eventdb.schema.sqlserver.SQLServerEventDBSchema
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.resolver.MigrationResolver

class EventDB(config: PersistenceConfig) extends LazyLogging {
  if (config.initializeDatabaseSchemas && config.eventDB.isJDBC) {
    val jdbcConfig = config.eventDB.jdbcConfig
    val tablePrefix = config.tablePrefix
    val flywayTableName = config.eventDB.schemaHistoryTable

    val schema: EventDBSchema = jdbcConfig.profile match {
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
      .dataSource(jdbcConfig.url, jdbcConfig.user, jdbcConfig.password)
      .baselineOnMigrate(true)
      .baselineVersion("0.0.0")
      .baselineDescription("CaseFabric EventDB")
      .table(flywayTableName)
      .resolvers((_: MigrationResolver.Context) => schema.migrationScripts(tablePrefix))

    // Create a connection and run migration
    val flyway = flywayConfiguration.load()
    flyway.migrate()
  }
}
