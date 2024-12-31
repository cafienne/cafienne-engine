/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.querydb.schema.versions.util

import org.cafienne.infrastructure.jdbc.cqrs.OffsetStoreTables
import org.cafienne.querydb.materializer.cases.CaseEventSink
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupEventSink
import org.cafienne.querydb.materializer.tenant.TenantEventSink
import org.cafienne.querydb.schema.QueryDBSchema

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

  lazy val resetConsentGroupEventOffset = {
    getResetterScript(ConsentGroupEventSink.offsetName)
  }

  def getResetterScript(projectionName: String) = {
    asSqlMigration(TableQuery[OffsetStoreTable].filter(_.name === projectionName).delete)
  }
}
