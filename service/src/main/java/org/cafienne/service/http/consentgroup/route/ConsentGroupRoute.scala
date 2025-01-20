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

import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.ConsentGroupUser
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.persistence.querydb.lastmodified.{Headers, LastModifiedHeader}
import org.cafienne.persistence.querydb.query.UserQueries
import org.cafienne.service.infrastructure.authentication.AuthenticatedUser
import org.cafienne.service.infrastructure.route.{CommandRoute, QueryRoute}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ConsentGroupRoute extends CommandRoute with QueryRoute {
  override val lastModifiedHeaderName: String = Headers.CONSENT_GROUP_LAST_MODIFIED
  val userQueries: UserQueries

  def consentGroupUser(subRoute: ConsentGroupUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { group =>
        readLastModifiedHeader(Headers.CONSENT_GROUP_LAST_MODIFIED) { lastModified =>
          onComplete(getConsentGroupUser(user, group, lastModified)) {
            case Success(groupUser) => subRoute(groupUser)
            case Failure(t) => throw t
          }
        }
      }
    }
  }

  def getConsentGroupUser(user: AuthenticatedUser, group: String, groupLastModified: LastModifiedHeader): Future[ConsentGroupUser] = {
    runSyncedQuery(userQueries.getConsentGroupUser(user, group), groupLastModified)
  }

  def askConsentGroup(command: ConsentGroupCommand): Route = askModelActor(command)
}
