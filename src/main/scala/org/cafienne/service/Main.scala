/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.querydb.schema.QueryDB
import org.cafienne.service.akkahttp.CafienneHttpServer
import org.cafienne.system.CaseSystem

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App with LazyLogging {
  try {
    // Create the Case System
    val caseSystem: CaseSystem = new CaseSystem
    implicit val system: ActorSystem = caseSystem.system
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    // Start running the Event Sinks
    QueryDB.open(caseSystem)

    // Create and start the http server
    new CafienneHttpServer(caseSystem).start().onComplete {
      case Success(answer) =>
        logger.warn(s"Running Cafienne version: ${Cafienne.version}")
        logger.warn(s"Akka HTTP Server available at $answer")
      case Failure(msg) =>
        logger.error(s"Starting Akka HTTP Server failed: $msg")
        System.exit(-1) // Also exit the JVM; what use do we have to keep running when there is no http available...
    }


    // Inform akka when we're going down.
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