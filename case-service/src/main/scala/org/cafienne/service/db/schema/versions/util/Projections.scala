package org.cafienne.service.db.schema.versions.util

import org.cafienne.infrastructure.jdbc.cqrs.OffsetStoreTables
import org.cafienne.service.db.materializer.cases.CaseEventSink
import org.cafienne.service.db.materializer.tenant.TenantEventSink
import org.cafienne.service.db.schema.QueryDBSchema

/**
  * Helper object to create a script that resets the projection offset, so that it can be rebuild with next db schema version
  */
object Projections extends QueryDBSchema
  with OffsetStoreTables {
  import dbConfig.profile.api._

  lazy val renameOffsets = {
    def updateName(oldName: String, newName: String) = {
      val query = TableQuery[OffsetStoreTable].filter(_.name === oldName).map(_.name)
      query.update(newName)
      // For some unclear reason, the binding of the parameter is not done in the resulting Sql, therefore doing it hardcoded here
      query.updateStatement.replace("?", s"'$newName'")
    }

    val updateCaseOffset = updateName("CaseProjectionsWriter", CaseEventSink.offsetName)
    val updateTenantName = updateName ("TenantProjectionsWriter", TenantEventSink.offsetName)

    asSqlMigration(updateCaseOffset, updateTenantName)
  }

  lazy val resetCaseEventOffset = {
    getResetterScript(CaseEventSink.offsetName)
  }

  lazy val resetTenantEventOffset = {
    getResetterScript(TenantEventSink.offsetName)
  }

  def getResetterScript(projectionName: String) = {
    asSqlMigration(TableQuery[OffsetStoreTable].filter(_.name === projectionName).delete)
  }
}
