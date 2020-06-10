package org.cafienne.service.db.migration.versions

import org.cafienne.service.db.migration.QueryDbMigrationConfig
import slick.migration.api.Migration
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}

object CafienneQueryDatabaseSchema extends QueryDbMigrationConfig {
  def schema(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]] = {
    (V1Migration.getMigrations ++ V1_1_5Migration.getMigrations ++ V1_1_6Migration.getMigrations)
  }
}
