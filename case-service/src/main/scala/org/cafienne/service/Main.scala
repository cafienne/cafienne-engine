/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.akka.BuildInfo
import org.cafienne.identity.IdentityCache
import org.cafienne.infrastructure.jdbc.OffsetStorageImpl
import org.cafienne.service.api.SwaggerHttpServiceRoute
import org.cafienne.service.api.cases.{CaseQueriesImpl, CasesRoute}
import org.cafienne.service.api.debug.DebugRoute
import org.cafienne.service.api.participants.{RegistrationRoutes, TenantQueriesImpl}
import org.cafienne.service.api.projection.cases.CaseProjectionsWriter
import org.cafienne.service.api.projection.participants.TenantProjectionsWriter
import org.cafienne.service.api.projection.slick.SlickRecordsPersistence
import org.cafienne.service.api.projection.task.TaskProjectionsWriter
import org.cafienne.service.api.repository.RepositoryRoute
import org.cafienne.service.api.tasks.{TaskQueriesImpl, TasksRoute}
import org.cafienne.service.db.migration.Migrate

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success} // required for combining routes

object Main extends App {
  try {
    Migrate.migrateDatabase()
    startup()
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      System.exit(-1)
  }

  def httpRoutesTimeout = Timeout(15 seconds) // This is the timeout that the http engine uses to wait for futures
  def caseSystemTimeout = Timeout(10 seconds) // This is the timeout that the routes use to interact with the case engine

  def startup(): Unit = {
    // Start case system
    CaseSystem.start()

    // Take some implicits from the case system
    implicit val timeout = httpRoutesTimeout
    implicit val system = CaseSystem.system
    implicit val ec = system.dispatcher

    // Tell akka when we're going down.
    sys addShutdownHook {
      println("Shutting down the case service")
      Await.result(system.terminate(), 20.seconds)
    }

    val taskQueries = new TaskQueriesImpl
    val caseQueries = new CaseQueriesImpl
    val userQueries = new TenantQueriesImpl
    val updater = new SlickRecordsPersistence
    val offsetStorage = new OffsetStorageImpl

    implicit val userCache = new IdentityCache(userQueries)

    val caseProjection = new CaseProjectionsWriter(updater, offsetStorage)
    caseProjection.start()
    val taskProjection = new TaskProjectionsWriter(updater, offsetStorage)
    taskProjection.start()
    val participantsProjection = new TenantProjectionsWriter(userQueries, updater, offsetStorage)
    participantsProjection.start()

    // When running with H2, you can start a debug web server on port 8082.
    checkH2InDebugMode()

    // Some routes assume the above created implicit writers
    val apiRoutes =
      new CasesRoute(caseQueries).route ~
      new TasksRoute(taskQueries).route ~
      new RegistrationRoutes(userQueries).route ~
      new RepositoryRoute().route ~
      new DebugRoute().route ~
      // Add the routes for the API documentation frontend.
      new SwaggerHttpServiceRoute(system).swaggerUIRoute

    val apiHost = system.settings.config.getString("cafienne.api.bindhost")
    val apiPort = system.settings.config.getInt("cafienne.api.bindport")
    val httpServer = Http().bindAndHandle(apiRoutes, apiHost, apiPort)
    httpServer onComplete {
      case Success(answer) ⇒ {
        system.log.info(s"service is done: $answer")
        system.log.info(s"Running [$BuildInfo]")
      }
      case Failure(msg) ⇒ {
        system.log.error(s"service failed: $msg")
        System.exit(-1) // Also exit the JVM; what use do we have to keep running when there is no http available...
      }
    }

  }

  private def checkH2InDebugMode()(implicit system:ActorSystem): Unit = {
    import org.h2.tools.Server

    if (CaseSystem.config.hasPath("query-db.debug") && CaseSystem.config.getBoolean("query-db.debug")) {
      Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
    }
  }
}