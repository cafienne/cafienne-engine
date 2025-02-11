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

package org.cafienne.service.http.consentgroup.route

import jakarta.ws.rs._
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.infrastructure.route.AuthenticatedRoute

@Path("consent-group")
class ConsentGroupRoutes(override val httpService: CaseEngineHttpServer) extends AuthenticatedRoute {
  override val prefix: String = "consent-group"

  addSubRoute(new ConsentGroupOwnersRoute(httpService))
  addSubRoute(new ConsentGroupMembersRoute(httpService))
}
