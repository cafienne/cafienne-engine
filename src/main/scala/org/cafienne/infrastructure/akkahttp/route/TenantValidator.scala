package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

trait TenantValidator extends AuthenticatedRoute {
  /**
    * Check that the tenant exists
    *
    * @param tenant
    * @param subRoute
    * @return
    */
  def validateTenant(tenant: String, subRoute: => Route): Route = {
    onComplete(userCache.getTenant(tenant)) {
      case Success(_) => subRoute
      case Failure(t: Throwable) => throw t
    }
  }
}
