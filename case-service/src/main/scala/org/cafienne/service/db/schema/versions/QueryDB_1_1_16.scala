package org.cafienne.service.db.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.service.db.schema.QueryDBSchema
import org.cafienne.service.db.schema.table.CaseTables
import org.cafienne.service.db.schema.versions.util.Projections

object QueryDB_1_1_16 extends DbSchemaVersion with QueryDBSchema
  with CaseTables {

  val version = "1.1.16"
  val migrations = Projections.renameOffsets
}
