/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant

import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider


@Api(value = "tenant", tags = Array("tenant"))
@Path("/tenant")
class TenantRoutes(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {
  val tenantAdministrationRoute = new TenantAdministrationRoute()(userCache)
  val participants = new TenantUsersAdministrationRoute(userQueries)(userCache)

  override def routes = pathPrefix("tenant") {
    tenantAdministrationRoute.routes ~
    participants.routes
  }

}
