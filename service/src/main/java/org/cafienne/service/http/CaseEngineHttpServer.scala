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

package org.cafienne.service.http

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.concat
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.service.http.anonymous.AnonymousRequestRoutes
import org.cafienne.service.http.cases.CasesRoutes
import org.cafienne.service.http.consentgroup.route.ConsentGroupRoutes
import org.cafienne.service.http.debug.DebugRoute
import org.cafienne.service.http.identifiers.route.IdentifierRoutes
import org.cafienne.service.http.platform.{CaseEngineHealthRoute, PlatformRoutes}
import org.cafienne.service.http.repository.RepositoryRoute
import org.cafienne.service.http.storage.StorageRoutes
import org.cafienne.service.http.swagger.SwaggerHttpServiceRoute
import org.cafienne.service.http.tasks.TaskRoutes
import org.cafienne.service.http.tenant.route.TenantRoutes
import org.cafienne.service.infrastructure.configuration.OIDCConfiguration
import org.cafienne.service.infrastructure.route.CaseServiceRoute
import org.cafienne.system.CaseSystem

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class CaseEngineHttpServer(val caseSystem: CaseSystem) extends LazyLogging {
  val oidcConfiguration = new OIDCConfiguration(caseSystem.config.api.security)

  val routes: ListBuffer[CaseServiceRoute] = new ListBuffer[CaseServiceRoute]()

  addRoute(new CaseEngineHealthRoute(this))
  addRoute(new CasesRoutes(this))
  addRoute(new IdentifierRoutes(this))
  addRoute(new TaskRoutes(this))
  addRoute(new TenantRoutes(this))
  addRoute(new ConsentGroupRoutes(this))
  addRoute(new PlatformRoutes(this))
  addRoute(new RepositoryRoute(this))
  addRoute(new StorageRoutes(this))
  addRoute(new DebugRoute(this))
  // Optionally add the anonymous route
  if (caseSystem.config.api.anonymousConfig.enabled) {
    addRoute(new AnonymousRequestRoutes(this))
  }

  /**
    * Method to extend the default routes that Cafienne exposes
    * @param caseServiceRoute The additional Route must extend CaseServiceRoute
    */
  def addRoute(caseServiceRoute: CaseServiceRoute): Unit = routes += caseServiceRoute

  def start(): Future[Http.ServerBinding] = {
    logger.info("Starting Cafienne HTTP Server - loading swagger documentation of the routes")

    // Create the route tree
    val apiRoutes = {
      // Find the API classes of the routes and pass them to Swagger
      val apiClasses = routes.flatMap(route => route.apiClasses())
      var mainRoute = new SwaggerHttpServiceRoute(this, apiClasses.toSet).route
      def routeAppender(route: Route): Unit = mainRoute = concat(mainRoute, route)
      routes.map(_.route).foreach(routeAppender)
      mainRoute
    }

    val apiHost = caseSystem.config.api.bindHost
    val apiPort = caseSystem.config.api.bindPort
    logger.info(s"Starting Cafienne HTTP Server - starting on $apiHost:$apiPort")
    implicit val system: ActorSystem = caseSystem.system

    val httpServer = Http().newServerAt(apiHost, apiPort)
    httpServer.bindFlow(apiRoutes)
  }
}
