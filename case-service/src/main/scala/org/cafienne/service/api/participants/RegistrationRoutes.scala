/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.participants

import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider


@Api(value = "registration", tags = Array("registration"))
@Path("/registration")
class RegistrationRoutes(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {
  val tenantAdministrationRoute = new TenantAdministrationRoute()
  val participants = new TenantUsersAdministrationRoute(userQueries)
  val platform = new PlatformAdministrationRoute()

  override def routes = pathPrefix("registration") {
    platform.routes ~
    tenantAdministrationRoute.routes ~
    participants.routes
  }

}
