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

package org.cafienne.service.akkahttp.consentgroup.route

import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.ConsentGroupUser
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.querydb.materializer.LastModifiedRegistration
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupReader
import org.cafienne.querydb.query.UserQueries
import org.cafienne.service.akkahttp.Headers

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ConsentGroupRoute extends CommandRoute with QueryRoute {
  override val lastModifiedRegistration: LastModifiedRegistration = ConsentGroupReader.lastModifiedRegistration
  override val lastModifiedHeaderName: String = Headers.CONSENT_GROUP_LAST_MODIFIED
  val userQueries: UserQueries

  def consentGroupUser(subRoute: ConsentGroupUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { group =>
        optionalHeaderValueByName(Headers.CONSENT_GROUP_LAST_MODIFIED) { lastModified =>
          onComplete(getConsentGroupUser(user, group, lastModified)) {
            case Success(groupUser) => subRoute(groupUser)
            case Failure(t) => throw t
          }
        }
      }
    }
  }

  def getConsentGroupUser(user: AuthenticatedUser, group: String, groupLastModified: Option[String]): Future[ConsentGroupUser] = {
    runSyncedQuery(userQueries.getConsentGroupUser(user, group), groupLastModified)
  }

  def askConsentGroup(command: ConsentGroupCommand): Route = askModelActor(command)
}
