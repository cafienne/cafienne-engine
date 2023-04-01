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
import org.cafienne.board.state.definition.TeamDefinition
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.json.{Value, ValueMap}
import org.cafienne.querydb.query.{TenantQueriesImpl, UserQueries}
import org.cafienne.service.akkahttp.{ConsentGroupLastModifiedHeader, Headers, LastModifiedHeader}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait BoardRoute extends CommandRoute with QueryRoute {
  override val lastModifiedHeaderName: String = Headers.BOARD_LAST_MODIFIED
  val userQueries: UserQueries = new TenantQueriesImpl

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

  def asJson(map: Map[String, _]): ValueMap = Value.convert(map).asMap()

  def asJson(map: Option[Map[String, _]]): Option[ValueMap] = map.map(Value.convert(_).asMap())

  def getBoardUser(user: AuthenticatedUser, boardId: String, header: LastModifiedHeader): Future[BoardUser] = {
    // We need to check if the board header has the team extension or not. If not, then we directly query the consent group information on the user,
    //  otherwise, apparently last action on board was a board-team action, which resulted in a consent group update, and then we need to first await the event handling in the group projection.
    val optionalValue = header.lastModified.flatMap(blm =>
        if (blm.actorId.endsWith(TeamDefinition.EXTENSION)) {
          Some(blm.toString)
        } else {
          None
        })
    val groupLastModified = ConsentGroupLastModifiedHeader(optionalValue)
    runSyncedQuery(userQueries.getConsentGroupUser(user, boardId + TeamDefinition.EXTENSION), groupLastModified).map(consentGroupUser => {
      // TODO: extend board user with the roles the user has in the board, in order to be able to enrich the CaseUserIdentity on flows.
      BoardUser(consentGroupUser.id, boardId)
    })
  }

  def askBoard(command: BoardCommand): Route = {
    askModelActor(command)
  }
}
