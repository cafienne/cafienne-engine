package org.cafienne.service.db.migration

import slick.migration.api.Migration
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}

trait DbSchemaVersion extends QueryDbMigrationConfig {
  val version: String
  val migrations: Migration
  def getScript(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]] = {
    Seq(VersionedMigration(version, migrations))
  }
}
