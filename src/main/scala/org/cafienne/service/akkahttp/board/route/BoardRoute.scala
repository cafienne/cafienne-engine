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

package org.cafienne.service.akkahttp.board.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.querydb.query.board.{BoardQueries, BoardQueriesImpl}
import org.cafienne.service.akkahttp.Headers
import org.cafienne.tenant.actorapi.command.TenantCommand

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait BoardRoute extends CommandRoute with QueryRoute {
  val boardQueries: BoardQueries = new BoardQueriesImpl

  override val lastModifiedHeaderName: String = Headers.TENANT_LAST_MODIFIED

  def boardUser(subRoute: TenantUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { group =>
        optionalHeaderValueByName(Headers.TENANT_LAST_MODIFIED) { lastModified =>
          onComplete(getBoardUser(user, group, lastModified)) {
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

  def getBoardUser(user: AuthenticatedUser, tenant: String, lastModified: Option[String]): Future[TenantUser] = {
    //runSyncedQuery(boardQueries.getTeamByBoards(user, tenant), lastModified)
    Future.successful(TenantUser("id", "tenant", Set.empty, true, "Board User", "lana@example.com", true))
  }

  def askBoard(command: TenantCommand): Route = {
    askModelActor(command)
  }
}
