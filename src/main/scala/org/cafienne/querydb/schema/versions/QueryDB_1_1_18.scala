package org.cafienne.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.{CaseTables, ConsentGroupTables, TenantTables}
import org.cafienne.querydb.schema.versions.util.Projections
import slick.migration.api.TableMigration

object QueryDB_1_1_18 extends DbSchemaVersion with QueryDBSchema
  with TenantTables {

  val version = "1.1.18"
  val migrations = addUserRoleTenantIndex

  import dbConfig.profile.api._

  def addUserRoleTenantIndex = TableMigration(TableQuery[UserRoleTable]).addIndexes(_.indexUserRoleTenant)

}
