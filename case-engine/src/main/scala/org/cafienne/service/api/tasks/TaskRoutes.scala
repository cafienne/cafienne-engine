/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tasks

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.service.db.query.TaskQueries
import org.cafienne.system.CaseSystem

import javax.ws.rs.Path

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tasks")
class TaskRoutes(val taskQueries: TaskQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends TaskRoute {
  override val prefix = "tasks"

  addSubRoute(new TaskQueryRoutes(taskQueries))
  addSubRoute(new TaskActionRoutes(taskQueries))
}
