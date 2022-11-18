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

package org.cafienne.service.akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akkahttp.route.CaseServiceRoute
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

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class CafienneHttpServer(val caseSystem: CaseSystem) extends LazyLogging {

  val defaultRoutes: Seq[CaseServiceRoute] = {
    val routes = Seq(new CaseEngineHealthRoute(caseSystem),
      new CasesRoutes(caseSystem),
      new IdentifierRoutes(caseSystem),
      new TaskRoutes(caseSystem),
      new TenantRoutes(caseSystem),
      new ConsentGroupRoutes(caseSystem),
      new PlatformRoutes(caseSystem),
      new RepositoryRoute(caseSystem),
      new DebugRoute(caseSystem),
    )
    // Optionally add the anonymous route
    if (Cafienne.config.api.anonymousConfig.enabled) {
      routes ++ Seq(new AnonymousRequestRoutes(caseSystem))
    } else {
      routes
    }
  }
  val routes: ListBuffer[CaseServiceRoute] = new ListBuffer[CaseServiceRoute]().addAll(defaultRoutes)

  def addRoute(caseServiceRoute: CaseServiceRoute): Unit = routes += caseServiceRoute

  def start(): Future[Http.ServerBinding] = {
    logger.info("Starting Cafienne HTTP Server - loading swagger documentation of the routes")

    // Create the route tree
    val apiRoutes = {
      // Find the API classes of the routes and pass them to Swagger
      val apiClasses = routes.flatMap(route => route.apiClasses())
      var mainRoute = new SwaggerHttpServiceRoute(apiClasses.toSet).route
      def routeAppender(route: Route) = mainRoute = concat(mainRoute, route)
      routes.map(_.route).foreach(routeAppender)
      mainRoute
    }

    val apiHost = Cafienne.config.api.bindHost
    val apiPort = Cafienne.config.api.bindPort
    logger.info(s"Starting Cafienne HTTP Server - starting akka http on $apiHost:$apiPort")
    implicit val system: ActorSystem = caseSystem.system

    val akkaHttp = Http().newServerAt(apiHost, apiPort)
    akkaHttp.bindFlow(apiRoutes)
  }
}
