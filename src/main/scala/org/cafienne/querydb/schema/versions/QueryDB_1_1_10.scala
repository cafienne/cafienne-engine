package org.cafienne.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.CaseTables
import slick.migration.api.TableMigration

object QueryDB_1_1_10 extends DbSchemaVersion with QueryDBSchema
  with CaseTables {

  val version = "1.1.10"
  val migrations = (
    addPlanItemDefinitionIdColumn
  )

  import dbConfig.profile.api._

  def addPlanItemDefinitionIdColumn = TableMigration(TableQuery[PlanItemTable]).addColumns(_.definitionId)

}
