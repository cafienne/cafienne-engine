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

package org.cafienne.persistence.querydb.schema.versions.util

import org.cafienne.persistence.infrastructure.jdbc.cqrs.OffsetStoreTables
import org.cafienne.persistence.infrastructure.jdbc.schema.SlickMigrationExtensions
import org.cafienne.persistence.querydb.materializer.cases.CaseEventSink
import org.cafienne.persistence.querydb.materializer.consentgroup.ConsentGroupEventSink
import org.cafienne.persistence.querydb.materializer.tenant.TenantEventSink
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

/**
  * Helper object to create a script that resets the projection offset, so that it can be rebuild with next db schema version
  */
class Projections(val dbConfig: DatabaseConfig[JdbcProfile], val tablePrefix: String)
  extends OffsetStoreTables
  with SlickMigrationExtensions {

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
