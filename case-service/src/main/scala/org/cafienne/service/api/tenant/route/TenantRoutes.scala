/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.UserQueries

@Path("/tenant")
class TenantRoutes(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {
  override val prefix: String = "tenant"

  addSubRoute(new TenantOwnersRoute(userQueries)(userCache))
  addSubRoute(new TenantUsersRoute(userQueries)(userCache))
  addSubRoute(new DeprecatedTenantOwnersRoute(userQueries)(userCache))
}
