package org.cafienne.infrastructure.jdbc.schema

import org.cafienne.infrastructure.jdbc.CafienneJDBCConfig
import slick.migration.api.Migration
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}

trait DbSchemaVersion extends CafienneJDBCConfig {
  val version: String
  val migrations: Migration
  def getScript(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]] = {
    Seq(VersionedMigration(version, migrations))
  }
}
