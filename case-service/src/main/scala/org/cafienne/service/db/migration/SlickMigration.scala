package org.cafienne.service.db.migration

import org.cafienne.infrastructure.jdbc.DbConfig
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}
import slick.migration.api.{H2Dialect, Migration, PostgresDialect}

trait MigrationConfig extends DbConfig {
  implicit val dialect = getDialect(dbConfig.config.getString("profile"))

  private def getDialect(profile: String) = {
    profile match {
      case p if p.toLowerCase().contains("h2") => new H2Dialect
      case _ => new PostgresDialect
    }
  }
}

trait SlickMigration extends MigrationConfig {
  def getMigrations(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]]
}
