/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.akka.BuildInfo
import org.cafienne.identity.IdentityCache
import org.cafienne.infrastructure.jdbc.OffsetStorageImpl
import org.cafienne.service.api.SwaggerHttpServiceRoute
import org.cafienne.service.api.cases.{CaseQueriesImpl, CasesRoute}
import org.cafienne.service.api.debug.DebugRoute
import org.cafienne.service.api.participants.{RegistrationRoutes, UserQueriesImpl}
import org.cafienne.service.api.projection.cases.CaseProjectionsWriter
import org.cafienne.service.api.projection.participants.TenantProjectionsWriter
import org.cafienne.service.api.projection.slick.SlickRecordsPersistence
import org.cafienne.service.api.projection.task.TaskProjectionsWriter
import org.cafienne.service.api.repository.RepositoryRoute
import org.cafienne.service.api.tasks.{TaskQueriesImpl, TasksRoute}
import org.cafienne.service.db.events.EventDatabaseProvider
import org.cafienne.service.db.migration.Migrate
import org.h2.tools.Server

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success} // required for combining routes

object Main extends App {

  try {
    //EventDatabaseProvider.createOrMigrate
    Migrate.migrateDatabase()
    startup(Seq("2551"))
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      System.exit(-1)
  }

  def httpRoutesTimeout = Timeout(15 seconds) // This is the timeout that the http engine uses to wait for futures
  def caseSystemTimeout = Timeout(10 seconds) // This is the timeout that the routes use to interact with the case engine

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      // Create an Akka system
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      implicit val system = ActorSystem("ClusterSystem", config)
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher

      implicit val timeout = httpRoutesTimeout

      sys addShutdownHook {
        println("Shutting down this case service")
        Await.result(system.terminate(), 20.seconds)
      }

      if (system.settings.config.getString("akka.persistence.journal.plugin").endsWith("leveldb")) {
        system.log.warning("LEVELDB event store selected, please note that this setup is NOT capable of clustering the case service ! (use the cassandra store instead)")
      }

      if (system.settings.config.getString("akka.persistence.journal.plugin").contains("leveldb-shared")) {
        system.log.error("LEVELDB-SHARED is NOT FUNCTIONAL with the case service. Please change to leveldb or - for clustered setup - the cassandra plugin")
      }
      // Start case system in clustered mode.
      CaseSystem.startCluster()

      Thread.sleep(3000)

      val taskQueries = new TaskQueriesImpl
      val caseQueries = new CaseQueriesImpl
      val userQueries = new UserQueriesImpl
      val updater = new SlickRecordsPersistence
      val offsetStorage = new OffsetStorageImpl

      implicit val userCache = new IdentityCache(userQueries)

      val caseProjection = new CaseProjectionsWriter(updater, offsetStorage)
      caseProjection.start()
      val taskProjection = new TaskProjectionsWriter(updater, offsetStorage)
      taskProjection.start()
      val participantsProjection = new TenantProjectionsWriter(userQueries, updater, offsetStorage)
      participantsProjection.start()

      if (system.settings.config.hasPath("projectionsDB.debug")
        && system.settings.config.getBoolean("projectionsDB.debug")) {
        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
      }

      val apiRoutes =
        // Some routes assume the above created implicit writers
        new CasesRoute(caseQueries, CaseSystem.caseMessageRouter).route ~
        new TasksRoute(taskQueries, CaseSystem.caseMessageRouter).route ~
        new RegistrationRoutes(userQueries, CaseSystem.tenantMessageRouter()).route ~
        new RepositoryRoute().route ~
        new DebugRoute().route ~
        // Add the routes for the API documentation frontend.
        new SwaggerHttpServiceRoute(system, materializer).swaggerUIRoute

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
  }

}