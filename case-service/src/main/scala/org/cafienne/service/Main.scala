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
import org.cafienne.BuildInfo
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.config.Cafienne
import org.cafienne.identity.IdentityCache
import org.cafienne.infrastructure.akka.http.route.CaseServiceRoute
import org.cafienne.infrastructure.jdbc.cqrs.QueryDBOffsetStorageProvider
import org.cafienne.service.api.anonymous.AnonymousRequestRoutes
import org.cafienne.service.api.cases.route.CasesRoutes
import org.cafienne.service.api.debug.DebugRoute
import org.cafienne.service.api.identifiers.route.IdentifierRoutes
import org.cafienne.service.api.platform.{BootstrapPlatformConfiguration, CaseEngineHealthRoute, PlatformRoutes}
import org.cafienne.service.db.materializer.cases.CaseProjectionsWriter
import org.cafienne.service.db.query.{CaseQueriesImpl, IdentifierQueriesImpl, TaskQueriesImpl, TenantQueriesImpl}
import org.cafienne.service.db.materializer.slick.SlickRecordsPersistence
import org.cafienne.service.db.materializer.tenant.TenantProjectionsWriter
import org.cafienne.service.api.repository.RepositoryRoute
import org.cafienne.service.api.swagger.SwaggerHttpServiceRoute
import org.cafienne.service.api.tasks.TaskRoutes
import org.cafienne.service.api.tenant.route.TenantRoutes
import org.cafienne.service.db.schema.QueryDB

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success} // required for combining routes

object Main extends App {
  try {
    QueryDB.verifyConnectivity()
    startup()
  } catch {
    case t: Throwable => stop(t)
  }

  def stop(t: Throwable) = {
    t.printStackTrace()
    System.exit(-1)
  }

  def httpRoutesTimeout = Timeout(15 seconds) // This is the timeout that the http engine uses to wait for futures
  def caseSystemTimeout = Timeout(10 seconds) // This is the timeout that the routes use to interact with the case engine

  def startup(): Unit = {
    // Take some implicits from the case system
    implicit val caseSystem = new CaseSystem
    implicit val timeout = httpRoutesTimeout
    implicit val system = caseSystem.system
    implicit val ec = system.dispatcher

    // Tell akka when we're going down.
    sys addShutdownHook {
      println("Shutting down the case service")
      Await.result(system.terminate(), 20.seconds)
    }

    // First, start platform bootstrap configuration
    BootstrapPlatformConfiguration.run(caseSystem)

    val taskQueries = new TaskQueriesImpl
    val caseQueries = new CaseQueriesImpl
    val identifierQueries = new IdentifierQueriesImpl
    val userQueries = new TenantQueriesImpl
    val updater = new SlickRecordsPersistence
    val offsetStorage = new QueryDBOffsetStorageProvider

    implicit val userCache = new IdentityCache(userQueries)

    new CaseProjectionsWriter(updater, offsetStorage).start()
    new TenantProjectionsWriter(userQueries, updater, offsetStorage).start()

    // When running with H2, you can start a debug web server on port 8082.
    checkH2InDebugMode()

    // Some routes assume the above created implicit writers
    val caseServiceRoutes: Seq[CaseServiceRoute] = {
      val fixedRoutes = Seq(
        new CaseEngineHealthRoute(),
        new CasesRoutes(caseQueries),
        new IdentifierRoutes(identifierQueries),
        new TaskRoutes(taskQueries),
        new TenantRoutes(userQueries),
        new PlatformRoutes(),
        new RepositoryRoute(),
        new DebugRoute()
      )
      Cafienne.config.api.anonymousConfig.enabled match {
        case true => fixedRoutes ++ Seq(new AnonymousRequestRoutes())
        case false => fixedRoutes
      }
    }

    // Find the API classes of the routes and pass them to Swagger
    val apiClasses = caseServiceRoutes.flatMap(route => route.apiClasses)

    // Create the route tree
    val apiRoutes = {
      var mainRoute = new SwaggerHttpServiceRoute(apiClasses.toSet).route
      caseServiceRoutes.map(c => c.route).foreach(route => mainRoute = concat(mainRoute, route))
      mainRoute
    }

    val apiHost = Cafienne.config.api.bindHost
    val apiPort = Cafienne.config.api.bindPort
    val httpServer = Http().newServerAt(apiHost, apiPort).bindFlow(apiRoutes)
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

    if (Cafienne.config.queryDB.debug) {
      val port = "8082"
      system.log.warning("Starting H2 Web Client on port " + port)
      Server.createWebServer("-web", "-webAllowOthers", "-webPort", port).start()
    }
  }
}