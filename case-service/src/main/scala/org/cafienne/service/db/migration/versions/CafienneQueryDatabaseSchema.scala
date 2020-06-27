package org.cafienne.service.db.migration.versions

import org.cafienne.service.db.migration.QueryDbMigrationConfig
import slick.migration.api.Migration
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}


/**
  * To quickly DROP all tables (including the flyway tables) from Postgres, run the following script
  * *
   DROP table case_file CASCADE;
   DROP table case_instance CASCADE;
   DROP table case_instance_definition CASCADE;
   DROP table case_instance_role CASCADE;
   DROP table case_instance_team_member CASCADE;
   DROP table flyway_schema_history CASCADE;
   DROP table plan_item CASCADE;
   DROP table plan_item_history CASCADE;
   DROP table task CASCADE;
   DROP table "tenant" CASCADE;
   DROP table "tenant_owners" CASCADE;
   DROP table user_role CASCADE;
   DROP table offset_storage CASCADE;
  */

object CafienneQueryDatabaseSchema extends QueryDbMigrationConfig {
  def schema(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]] = {
    Seq(
      QueryDB_1_0_0,
      QueryDB_1_1_5,
      QueryDB_1_1_6,
    ).flatMap(schema => schema.getScript)
  }
}
