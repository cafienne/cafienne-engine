package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait TenantValidator extends AuthenticatedRoute {
  implicit val userCache: IdentityProvider
  implicit val ec: ExecutionContext

  /**
    * Check that the tenant exists
    * @param tenant
    * @param subRoute
    * @return
    */
  def validateTenant(tenant: String, subRoute: () => Route): Route = {
    onComplete(userCache.getTenant(tenant)) {
      case Success(_) => subRoute()
      case Failure(t: Throwable) => throw t
    }
  }
}
