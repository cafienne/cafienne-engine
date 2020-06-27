package org.cafienne.service.db.migration.versions

import org.cafienne.service.db.migration.{DbSchemaVersion, Projections}
import slick.migration.api.Migration

object QueryDB_1_1_5 extends DbSchemaVersion {
  val version = "1.1.5"
  val migrations: Migration = Projections.resetCaseProjectionWriter
}
