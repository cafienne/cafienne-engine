/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service

import akka.actor.ActorSystem
import akka.http.javadsl.server.Directives
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.akka.BuildInfo
import org.cafienne.identity.IdentityCache
import org.cafienne.infrastructure.akka.http.route.CaseServiceRoute
import org.cafienne.infrastructure.jdbc.{JDBCBasedOffsetStorageProvider, OffsetStorageImpl}
import org.cafienne.service.api.SwaggerHttpServiceRoute
import org.cafienne.service.api.cases.CaseQueriesImpl
import org.cafienne.service.api.cases.route.CasesRoutes
import org.cafienne.service.api.debug.DebugRoute
import org.cafienne.service.api.platform.{BootstrapPlatformConfiguration, PlatformRoute}
import org.cafienne.service.api.tenant.TenantQueriesImpl
import org.cafienne.service.api.projection.cases.CaseProjectionsWriter
import org.cafienne.service.api.projection.participants.TenantProjectionsWriter
import org.cafienne.service.api.projection.slick.SlickRecordsPersistence
import org.cafienne.service.api.projection.task.TaskProjectionsWriter
import org.cafienne.service.api.registration.FormerRegistrationRoutes
import org.cafienne.service.api.repository.RepositoryRoute
import org.cafienne.service.api.tasks.{TaskQueriesImpl, TaskRoutes}
import org.cafienne.service.api.tenant.route.{TenantOwnersRoute, TenantRoutes, TenantUsersRoute}
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

    // First, start platform bootstrap configuration
    BootstrapPlatformConfiguration.run()

    val taskQueries = new TaskQueriesImpl
    val caseQueries = new CaseQueriesImpl
    val userQueries = new TenantQueriesImpl
    val updater = new SlickRecordsPersistence
    val offsetStorage = new JDBCBasedOffsetStorageProvider

    implicit val userCache = new IdentityCache(userQueries)

    new CaseProjectionsWriter(updater, offsetStorage).start()
    new TaskProjectionsWriter(updater, offsetStorage).start()
    new TenantProjectionsWriter(userQueries, updater, offsetStorage).start()

    // When running with H2, you can start a debug web server on port 8082.
    checkH2InDebugMode()

    // Some routes assume the above created implicit writers
    val caseServiceRoutes: Seq[CaseServiceRoute] = Seq(
      new CasesRoutes(caseQueries),
      new TaskRoutes(taskQueries),
      new TenantRoutes(userQueries),
      new PlatformRoute(),
      new RepositoryRoute(),
      new DebugRoute()
    )

    // Find the API classes of the routes and pass the to Swagger
    val apiClasses = caseServiceRoutes.flatMap(route => route.apiClasses)

    // For unclear reasons we cannot map the Seq(CaseServiceRoute)
    val apiRoutes = {
      caseServiceRoutes.toArray.apply(0).route ~
        caseServiceRoutes.toArray.apply(1).route ~
        caseServiceRoutes.toArray.apply(2).route ~
        caseServiceRoutes.toArray.apply(3).route ~
        caseServiceRoutes.toArray.apply(4).route ~
        caseServiceRoutes.toArray.apply(5).route ~
//      mainRoute ~
      // Add the routes for the API documentation frontend.
      new SwaggerHttpServiceRoute(apiClasses.toSet).route ~
      // For as long as it is needed... preferably remove them.
      new FormerRegistrationRoutes(userQueries).route
    }

    // UNCLEAR why below does not work. It compiles, it runs, but it does not do what we want it to do (i.e., tests are failing with "route not found 404")
    //    caseServiceRoutes.foreach(route => apiRoutes ~ {
    //      println("\n\nAdding route from "+route.getClass.getSimpleName)
    //      val r = route.route
    //      println("it is: "+r)
    //      route.route
    //    })


    val apiHost = CaseSystem.config.api.bindHost
    val apiPort = CaseSystem.config.api.bindPort
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

    if (CaseSystem.config.queryDB.debug) {
      Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
    }
  }
}