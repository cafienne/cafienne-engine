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

package org.cafienne.persistence.querydb.schema

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.infrastructure.jdbc.schema.{CustomMigrationInfo, QueryDBSchemaVersion, SlickMigrationExtensions}
import org.cafienne.persistence.querydb.materializer.cases.CaseEventSink
import org.cafienne.persistence.querydb.materializer.consentgroup.ConsentGroupEventSink
import org.cafienne.persistence.querydb.materializer.slick.QueryDBWriter
import org.cafienne.persistence.querydb.materializer.tenant.TenantEventSink
import org.cafienne.persistence.querydb.schema.versions._
import org.cafienne.system.CaseSystem
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

class QueryDB(val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("", Cafienne.config.persistence.queryDB.config))
  extends SlickMigrationExtensions
    with LazyLogging {

  val writer = new QueryDBWriter(this)

  def startEventSinks(caseSystem: CaseSystem): Unit = {
    new CaseEventSink(caseSystem.system, writer).start()
    new TenantEventSink(caseSystem, writer).start()
    new ConsentGroupEventSink(caseSystem, writer).start()

    // When running with H2, you can start a debug web server on port 8082.
    checkH2InDebugMode()
  }

  private def checkH2InDebugMode(): Unit = {
    import org.h2.tools.Server

    if (Cafienne.config.persistence.queryDB.debug) {
      val port = "8082"
      logger.warn("Starting H2 Web Client on port " + port)
      Server.createWebServer("-web", "-webAllowOthers", "-webPort", port).start()
    }
  }

  import dbConfig.profile.api._
  import org.flywaydb.core.api.output.MigrateResult
  import slick.migration.api.Migration
  import slick.migration.api.flyway.{MigrationInfo, SlickFlyway}

  import scala.concurrent.Await
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration.DurationInt
  implicit val infoProvider: MigrationInfo.Provider[Migration] = CustomMigrationInfo.provider

  private def useSchema(schemas: QueryDBSchemaVersion*): MigrateResult = {
    try {
      val flywayConfiguration = SlickFlyway(db)(schemas.flatMap(schema => schema.getScript))
        .baselineOnMigrate(true)
        .baselineDescription("CaseFabric QueryDB")
        .baselineVersion("0.0.0")
        .table(Cafienne.config.persistence.queryDB.schemaHistoryTable)

      // Create a connection and run migration
      val flyway = flywayConfiguration.load()
      flyway.migrate()
    } catch {
      case e: Exception => {
        logger.error("An issue with migration happened", e)
        val my = sql"""select description from flyway_schema_history""".as[String]
        val res = db.stream(my)
        logger.debug(s"Migration contents:")
        // Wait 5 seconds to print the resulting errors before throwing the exception
        Await.result(res.foreach { r => logger.debug("Migration: {}", r)}, 5.seconds)
        throw e
      }
    }
  }

  def initializeDatabaseSchema(): MigrateResult = {
    useSchema(
      new QueryDB_1_0_0(dbConfig),
      new QueryDB_1_1_5(dbConfig),
      new QueryDB_1_1_6(dbConfig),
      new QueryDB_1_1_10(dbConfig),
      new QueryDB_1_1_11(dbConfig),
      new QueryDB_1_1_16(dbConfig),
      new QueryDB_1_1_18(dbConfig),
      new QueryDB_1_1_22(dbConfig),
    )
  }
}
