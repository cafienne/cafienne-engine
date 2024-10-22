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

package org.cafienne.infrastructure.http.route

import org.apache.pekko.http.scaladsl.server.Route

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
