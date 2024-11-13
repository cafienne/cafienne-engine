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

package com.casefabric.querydb.schema

import com.typesafe.scalalogging.LazyLogging
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.infrastructure.jdbc.schema.CaseFabricDatabaseDefinition
import com.casefabric.querydb.materializer.cases.CaseEventSink
import com.casefabric.querydb.materializer.consentgroup.ConsentGroupEventSink
import com.casefabric.querydb.materializer.slick.SlickQueryDB
import com.casefabric.querydb.materializer.tenant.TenantEventSink
import com.casefabric.querydb.schema.versions._
import com.casefabric.system.CaseSystem
import org.flywaydb.core.api.output.MigrateResult

object QueryDB extends CaseFabricDatabaseDefinition with QueryDBSchema with LazyLogging {
  def initializeDatabaseSchema(): MigrateResult = {
    useSchema(Seq(QueryDB_1_0_0, QueryDB_1_1_5, QueryDB_1_1_6, QueryDB_1_1_10, QueryDB_1_1_11, QueryDB_1_1_16, QueryDB_1_1_18, QueryDB_1_1_22))
  }

  def startEventSinks(caseSystem: CaseSystem): Unit = {
    new CaseEventSink(caseSystem.system, SlickQueryDB).start()
    new TenantEventSink(caseSystem, SlickQueryDB).start()
    new ConsentGroupEventSink(caseSystem, SlickQueryDB).start()

    // When running with H2, you can start a debug web server on port 8082.
    checkH2InDebugMode()
  }

  private def checkH2InDebugMode(): Unit = {
    import org.h2.tools.Server

    if (CaseFabric.config.persistence.queryDB.debug) {
      val port = "8082"
      logger.warn("Starting H2 Web Client on port " + port)
      Server.createWebServer("-web", "-webAllowOthers", "-webPort", port).start()
    }
  }
}
