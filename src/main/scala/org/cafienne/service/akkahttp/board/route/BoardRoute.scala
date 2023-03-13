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

import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.board.actorapi.command.BoardCommand
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.service.akkahttp.{Headers, LastModifiedHeader}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait BoardRoute extends CommandRoute with QueryRoute {
  override val lastModifiedHeaderName: String = Headers.BOARD_LAST_MODIFIED

  def boardUser(subRoute: BoardUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { boardId =>
        readLastModifiedHeader() { lastModified =>
          onComplete(getBoardUser(user, boardId, lastModified)) {
            case Success(boardUser) => subRoute(boardUser)
            case Failure(t) => throw t
          }
        }
      }
    }
  }

  def getBoardUser(user: AuthenticatedUser, boardId: String, lastModified: LastModifiedHeader): Future[BoardUser] = {
    //runSyncedQuery(boardQueries.getTeamByBoards(user, tenant), lastModified)
    Future.successful(BoardUser("userId", boardId))
  }

  def askBoard(command: BoardCommand): Route = {
    askModelActor(command)
  }
}
