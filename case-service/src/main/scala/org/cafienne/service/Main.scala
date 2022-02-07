/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service

import org.cafienne.service.akkahttp.CafienneHttpServer
import org.cafienne.system.CaseSystem

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  try {
    // Create the Case System
    val caseSystem: CaseSystem = new CaseSystem
    // Create and start the http server
    new CafienneHttpServer(caseSystem).start()

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