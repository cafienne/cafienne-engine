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

package com.casefabric.service.http

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.concat
import org.apache.pekko.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.service.http.anonymous.AnonymousRequestRoutes
import com.casefabric.service.http.cases.CasesRoutes
import com.casefabric.service.http.consentgroup.route.ConsentGroupRoutes
import com.casefabric.service.http.debug.DebugRoute
import com.casefabric.service.http.identifiers.route.IdentifierRoutes
import com.casefabric.service.http.platform.{CaseEngineHealthRoute, PlatformRoutes}
import com.casefabric.service.http.repository.RepositoryRoute
import com.casefabric.service.http.storage.StorageRoutes
import com.casefabric.service.http.swagger.SwaggerHttpServiceRoute
import com.casefabric.service.http.tasks.TaskRoutes
import com.casefabric.service.http.tenant.route.TenantRoutes
import com.casefabric.service.infrastructure.route.CaseServiceRoute
import com.casefabric.system.CaseSystem

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class CaseFabricHttpServer(val caseSystem: CaseSystem) extends LazyLogging {

  val routes: ListBuffer[CaseServiceRoute] = new ListBuffer[CaseServiceRoute]()

  addRoute(new CaseEngineHealthRoute(caseSystem))
  addRoute(new CasesRoutes(caseSystem))
  addRoute(new IdentifierRoutes(caseSystem))
  addRoute(new TaskRoutes(caseSystem))
  addRoute(new TenantRoutes(caseSystem))
  addRoute(new ConsentGroupRoutes(caseSystem))
  addRoute(new PlatformRoutes(caseSystem))
  addRoute(new RepositoryRoute(caseSystem))
  addRoute(new StorageRoutes(caseSystem))
  addRoute(new DebugRoute(caseSystem))
  // Optionally add the anonymous route
  if (CaseFabric.config.api.anonymousConfig.enabled) {
    addRoute(new AnonymousRequestRoutes(caseSystem))
  }

  /**
    * Method to extend the default routes that CaseFabric exposes
    * @param caseServiceRoute The additional Route must extend CaseServiceRoute
    */
  def addRoute(caseServiceRoute: CaseServiceRoute): Unit = routes += caseServiceRoute

  def start(): Future[Http.ServerBinding] = {
    logger.info("Starting CaseFabric HTTP Server - loading swagger documentation of the routes")

    // Create the route tree
    val apiRoutes = {
      // Find the API classes of the routes and pass them to Swagger
      val apiClasses = routes.flatMap(route => route.apiClasses())
      var mainRoute = new SwaggerHttpServiceRoute(apiClasses.toSet).route
      def routeAppender(route: Route): Unit = mainRoute = concat(mainRoute, route)
      routes.map(_.route).foreach(routeAppender)
      mainRoute
    }

    val apiHost = CaseFabric.config.api.bindHost
    val apiPort = CaseFabric.config.api.bindPort
    logger.info(s"Starting CaseFabric HTTP Server - starting on $apiHost:$apiPort")
    implicit val system: ActorSystem = caseSystem.system

    val httpServer = Http().newServerAt(apiHost, apiPort)
    httpServer.bindFlow(apiRoutes)
  }
}
