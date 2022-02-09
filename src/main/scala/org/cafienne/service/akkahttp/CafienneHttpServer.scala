package org.cafienne.service.akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.concat
import org.cafienne.BuildInfo
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
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class CafienneHttpServer(val caseSystem: CaseSystem) {
  implicit val system: ActorSystem = caseSystem.system
  implicit val ec: ExecutionContextExecutor = system.dispatcher
//  val userCache: IdentityCache = new IdentityCache()
  private val routes = new ListBuffer[CaseServiceRoute]()

  def addRoute(caseServiceRoute: CaseServiceRoute): Unit = {
    routes += caseServiceRoute
  }

  addRoute(new CaseEngineHealthRoute(caseSystem))
  addRoute(new CasesRoutes(caseSystem))
  addRoute(new IdentifierRoutes(caseSystem))
  addRoute(new TaskRoutes(caseSystem))
  addRoute(new TenantRoutes(caseSystem))
  addRoute(new ConsentGroupRoutes(caseSystem))
  addRoute(new PlatformRoutes(caseSystem))
  addRoute(new RepositoryRoute(caseSystem))
  addRoute(new DebugRoute(caseSystem))
  // Optionally add the anonymous route
  if (Cafienne.config.api.anonymousConfig.enabled) {
    addRoute(new AnonymousRequestRoutes(caseSystem))
  }

  def start(): Unit = {
    // Find the API classes of the routes and pass them to Swagger
    val apiClasses = routes.flatMap(route => route.apiClasses())

    // Create the route tree
    val apiRoutes = {
      var mainRoute = new SwaggerHttpServiceRoute(apiClasses.toSet).route
      routes.map(c => c.route).foreach(route => mainRoute = concat(mainRoute, route))
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
}
