/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.registration

import akka.http.scaladsl.server.Directives._
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.UserQueries
import org.cafienne.service.api.tenant.route.{FormerAddTenantUserRoute, TenantRoute}

@Path("/registration")
class FormerRegistrationRoutes(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {
  val tenantOwnersRoute = new FormerTenantAdministrationRoute()(userCache)
  val tenantUsersRoute = new FormerTenantUsersAdministrationRoute(userQueries)(userCache)
  val platform = new FormerPlatformAdministrationRoute()(userCache)

  override def routes = pathPrefix("registration") {
    platform.routes ~
    tenantOwnersRoute.routes ~
    tenantUsersRoute.routes
  }

}
