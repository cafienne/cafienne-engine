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

package org.cafienne.persistence.infrastructure.jdbc.schema

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.infrastructure.jdbc.CafienneJDBCConfig
import org.flywaydb.core.api.output.MigrateResult
import slick.migration.api.Migration
import slick.migration.api.flyway.{MigrationInfo, SlickFlyway}

import scala.concurrent.Await

/**
  * Simple flyway abstraction that can be used to define and validate a JDBC database schema
  */
trait CafienneDatabaseDefinition extends CafienneJDBCConfig with LazyLogging {
  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  implicit val infoProvider: MigrationInfo.Provider[Migration] = CustomMigrationInfo.provider

  def useSchema(schemas: Seq[DbSchemaVersion]): MigrateResult = {
    try {
      val flywayConfiguration = SlickFlyway(db)(schemas.flatMap(schema => schema.getScript))
        .baselineOnMigrate(true)
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
}
