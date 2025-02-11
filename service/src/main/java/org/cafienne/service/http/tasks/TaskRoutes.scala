/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.service.http.tasks

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.ws.rs.Path
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.infrastructure.route.AuthenticatedRoute

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/tasks")
class TaskRoutes(override val httpService: CaseEngineHttpServer) extends AuthenticatedRoute {
  override val prefix = "tasks"
  addSubRoute(new TaskQueryRoutes(httpService))
  addSubRoute(new TaskActionRoutes(httpService))
}
