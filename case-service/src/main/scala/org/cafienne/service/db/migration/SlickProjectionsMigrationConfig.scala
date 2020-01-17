package org.cafienne.service.db.migration

import org.cafienne.infrastructure.jdbc.ProjectionsDbConfig
import slick.jdbc.{JdbcProfile, SQLServerProfile}
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}
import slick.migration.api.{Dialect, GenericDialect, Migration}

trait ProjectionsMigrationConfig extends ProjectionsDbConfig {
  implicit val dialect = getDialect(dbConfig.profile)

  private def getDialect(profile: JdbcProfile) = {
    ExtendedGenericDialect(profile)
  }
}

class SQLServerDialect extends Dialect[SQLServerProfile]

object ExtendedGenericDialect {
  def apply(driver: JdbcProfile): Dialect[_ <: JdbcProfile] = driver match {
    case _: SQLServerProfile    => new SQLServerDialect
    case _ => GenericDialect(driver)
  }
}

trait SlickProjectionsMigrationConfig extends ProjectionsMigrationConfig {
  def getMigrations(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]]
}
