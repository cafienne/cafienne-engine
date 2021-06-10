package org.cafienne.service.db.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.service.db.schema.QueryDBSchema
import org.cafienne.service.db.schema.versions.util.Projections
import slick.migration.api.Migration

object QueryDB_1_1_5 extends DbSchemaVersion with QueryDBSchema {
  val version = "1.1.5"
  val migrations: Migration = Projections.resetCaseProjectionWriter
}
