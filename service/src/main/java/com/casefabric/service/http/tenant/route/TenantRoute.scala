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

package com.casefabric.service.http.tenant.route

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import com.casefabric.actormodel.identity.TenantUser
import com.casefabric.querydb.lastmodified.{Headers, LastModifiedHeader}
import com.casefabric.querydb.query.{TenantQueriesImpl, UserQueries}
import com.casefabric.service.infrastructure.authentication.AuthenticatedUser
import com.casefabric.service.infrastructure.route.{CommandRoute, QueryRoute}
import com.casefabric.tenant.actorapi.command.TenantCommand

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait TenantRoute extends CommandRoute with QueryRoute {
  val userQueries: UserQueries = new TenantQueriesImpl

  override val lastModifiedHeaderName: String = Headers.TENANT_LAST_MODIFIED

  def tenantUser(subRoute: TenantUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { tenant =>
        readLastModifiedHeader(Headers.TENANT_LAST_MODIFIED) { lastModified =>
          onComplete(getTenantUser(user, tenant, lastModified)) {
            case Success(tenantUser) =>
              if (tenantUser.enabled) {
                subRoute(tenantUser)
              } else {
                complete(StatusCodes.Unauthorized, s"The user account ${tenantUser.id} has been disabled")
              }
            case Failure(t) => throw t
          }
        }
      }
    }
  }

  def getTenantUser(user: AuthenticatedUser, tenant: String, lastModified: LastModifiedHeader): Future[TenantUser] = {
    runSyncedQuery(userQueries.getTenantUser(user, tenant), lastModified)
  }

  def askTenant(command: TenantCommand): Route = {
    askModelActor(command)
  }
}
