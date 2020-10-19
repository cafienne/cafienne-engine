package org.cafienne.service.db.querydb.schema

import org.cafienne.infrastructure.jdbc.OffsetStoreTables
import org.cafienne.service.db.querydb.QueryDBSchema
import slick.lifted.TableQuery
import slick.migration.api.{SqlMigration, TableMigration}

/**
  * Helper object to create a script that resets the projection offset, so that it can be rebuild with next db schema version
  */
object Projections extends QueryDBSchema
  with OffsetStoreTables {

  lazy val resetCaseProjectionWriter = {
    getResetterScript("CaseProjectionsWriter")
  }

  lazy val resetTaskProjectionWriter = {
    getResetterScript("TaskProjectionsWriter")
  }

  lazy val resetTenantProjectionWriter = {
    getResetterScript("TenantProjectionsWriter")
  }

  def getResetterScript(projectionName: String) = {
    val offsetStoreTable = TableMigration(TableQuery[OffsetStoreTable])
    SqlMigration(s"""DELETE FROM "${offsetStoreTable.tableInfo.schemaName.fold("")(s => s + ".") + offsetStoreTable.tableInfo.tableName}" where "name" = '$projectionName' """)
  }
}
