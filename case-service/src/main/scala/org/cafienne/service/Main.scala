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
import org.cafienne.BuildInfo
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akkahttp.authentication.IdentityCache
import org.cafienne.infrastructure.akkahttp.route.CaseServiceRoute
import org.cafienne.infrastructure.jdbc.cqrs.QueryDBOffsetStorageProvider
import org.cafienne.querydb.materializer.cases.CaseEventSink
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupEventSink
import org.cafienne.querydb.materializer.slick.SlickRecordsPersistence
import org.cafienne.querydb.materializer.tenant.TenantEventSink
import org.cafienne.querydb.query.{CaseQueriesImpl, IdentifierQueriesImpl, TaskQueriesImpl, TenantQueriesImpl}
import org.cafienne.querydb.schema.QueryDB
import org.cafienne.service.akkahttp.anonymous.AnonymousRequestRoutes
import org.cafienne.service.akkahttp.cases.route.CasesRoutes
import org.cafienne.service.akkahttp.consentgroup.route.ConsentGroupRoutes
import org.cafienne.service.akkahttp.debug.DebugRoute
import org.cafienne.service.akkahttp.identifiers.route.IdentifierRoutes
import org.cafienne.service.akkahttp.platform.{CaseEngineHealthRoute, PlatformRoutes}
import org.cafienne.service.akkahttp.repository.RepositoryRoute
import org.cafienne.service.akkahttp.swagger.SwaggerHttpServiceRoute
import org.cafienne.service.akkahttp.tasks.TaskRoutes
import org.cafienne.service.akkahttp.tenant.route.TenantRoutes
import org.cafienne.system.CaseSystem

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
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

  def startup(): Unit = {
    // Take some implicits from the case system
    implicit val caseSystem: CaseSystem = new CaseSystem
    implicit val system: ActorSystem = caseSystem.system
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    // Tell akka when we're going down.
    sys addShutdownHook {
      println("Shutting down the case service")
      Await.result(system.terminate(), 20.seconds)
    }

    val taskQueries = new TaskQueriesImpl
    val caseQueries = new CaseQueriesImpl
    val identifierQueries = new IdentifierQueriesImpl
    val userQueries = new TenantQueriesImpl
    val updater = new SlickRecordsPersistence
    val offsetStorage = new QueryDBOffsetStorageProvider

    implicit val userCache: IdentityCache = new IdentityCache(userQueries)

    new CaseEventSink(updater, offsetStorage).start()
    new TenantEventSink(updater, offsetStorage).start()
    new ConsentGroupEventSink(updater, offsetStorage).start()

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
        new ConsentGroupRoutes(userQueries),
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
    val apiClasses = caseServiceRoutes.flatMap(route => route.apiClasses())

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
      case Success(answer) => {
        system.log.info(s"service is done: $answer")
        system.log.info(s"Running [$BuildInfo]")
      }
      case Failure(msg) => {
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