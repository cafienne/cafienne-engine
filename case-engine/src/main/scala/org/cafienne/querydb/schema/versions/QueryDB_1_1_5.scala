package org.cafienne.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.querydb.schema.QueryDBSchema
import slick.migration.api.{Migration, SqlMigration}

object QueryDB_1_1_5 extends DbSchemaVersion with QueryDBSchema {
  val version = "1.1.5"
  val migrations: Migration = SqlMigration("""DELETE FROM "offset_storage" where "name" = 'CaseProjectionsWriter' """)
}
