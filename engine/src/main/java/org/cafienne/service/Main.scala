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

package org.cafienne.service

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.Persistence
import org.cafienne.querydb.schema.QueryDB
import org.cafienne.service.http.CafienneHttpServer
import org.cafienne.system.CaseSystem

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object Main extends App with LazyLogging {
  // Initialize the database schema for both query db and the event journal
  Persistence.initializeDatabaseSchemas()

  try {
    // Create the Case System
    val caseSystem: CaseSystem = new CaseSystem

    // Start running the Event Sinks
    QueryDB.startEventSinks(caseSystem)

    implicit val ec: ExecutionContextExecutor = caseSystem.system.dispatcher
    // Create and start the http server
    new CafienneHttpServer(caseSystem).start().onComplete {
      case Success(answer) =>
        logger.warn(s"Running Cafienne version: ${Cafienne.version}")
        logger.warn(s"Cafienne HTTP Server available at $answer")
      case Failure(msg) =>
        logger.error(s"Starting Cafienne HTTP Server failed: $msg")
        System.exit(-1) // Also exit the JVM; what use do we have to keep running when there is no http available...
    }


    // Inform actor system when we're going down.
    sys addShutdownHook {
      println("Shutting down the case service")
      Await.result(caseSystem.system.terminate(), 20.seconds)
    }
  } catch {
    case t: Throwable =>
      // Always print stack trace
      t.printStackTrace()
      System.exit(-1)
  }
}