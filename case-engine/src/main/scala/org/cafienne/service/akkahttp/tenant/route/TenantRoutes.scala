/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tenant.route

import org.cafienne.system.CaseSystem

import javax.ws.rs._

@Path("/tenant")
class TenantRoutes(override val caseSystem: CaseSystem) extends TenantRoute {
  override val prefix: String = "tenant"

  addSubRoute(new TenantOwnersRoute(caseSystem))
  addSubRoute(new TenantUsersRoute(caseSystem))
}
