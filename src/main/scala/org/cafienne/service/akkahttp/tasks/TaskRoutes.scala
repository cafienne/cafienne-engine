/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tasks

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.infrastructure.akkahttp.route.AuthenticatedRoute
import org.cafienne.system.CaseSystem

import javax.ws.rs.Path

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tasks")
class TaskRoutes(override val caseSystem: CaseSystem) extends AuthenticatedRoute {
  override val prefix = "tasks"
  addSubRoute(new TaskQueryRoutes(caseSystem))
  addSubRoute(new TaskActionRoutes(caseSystem))
}
