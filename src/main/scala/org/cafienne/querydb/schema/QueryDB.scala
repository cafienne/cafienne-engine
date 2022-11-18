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

package org.cafienne.querydb.schema

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.jdbc.schema.CafienneDatabaseDefinition
import org.cafienne.querydb.materializer.cases.CaseEventSink
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupEventSink
import org.cafienne.querydb.materializer.slick.SlickQueryDB
import org.cafienne.querydb.materializer.tenant.TenantEventSink
import org.cafienne.querydb.schema.versions._
import org.cafienne.system.CaseSystem
import org.flywaydb.core.api.output.MigrateResult

object QueryDB extends CafienneDatabaseDefinition with QueryDBSchema with LazyLogging {
  def verifyConnectivity(): MigrateResult = {
    useSchema(Seq(QueryDB_1_0_0, QueryDB_1_1_5, QueryDB_1_1_6, QueryDB_1_1_10, QueryDB_1_1_11, QueryDB_1_1_16, QueryDB_1_1_18, QueryDB_1_1_22))
  }

  def open(caseSystem: CaseSystem): Unit = {
    verifyConnectivity()

    new CaseEventSink(caseSystem.system, SlickQueryDB).start()
    new TenantEventSink(caseSystem, SlickQueryDB).start()
    new ConsentGroupEventSink(caseSystem, SlickQueryDB).start()

    // When running with H2, you can start a debug web server on port 8082.
    checkH2InDebugMode()
  }

  private def checkH2InDebugMode(): Unit = {
    import org.h2.tools.Server

    if (Cafienne.config.queryDB.debug) {
      val port = "8082"
      logger.warn("Starting H2 Web Client on port " + port)
      Server.createWebServer("-web", "-webAllowOthers", "-webPort", port).start()
    }
  }
}
