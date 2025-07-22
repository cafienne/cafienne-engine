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
import org.cafienne.userregistration.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.persistence.infrastructure.lastmodified.Headers
import org.cafienne.persistence.querydb.query.tenant.ConsentGroupQueries
import org.cafienne.persistence.querydb.query.tenant.implementation.ConsentGroupQueriesImpl
import org.cafienne.service.infrastructure.route.{CommandRoute, QueryRoute}

import scala.util.{Failure, Success}

trait ConsentGroupRoute extends CommandRoute with QueryRoute {
  override val lastModifiedHeaderName: String = Headers.CONSENT_GROUP_LAST_MODIFIED
  val consentGroupQueries: ConsentGroupQueries = new ConsentGroupQueriesImpl(caseSystem.queryDB)

  def consentGroupUser(subRoute: ConsentGroupUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { group =>
        readLastModifiedHeader(Headers.CONSENT_GROUP_LAST_MODIFIED) { lastModified =>
          onComplete(runSyncedQuery(consentGroupQueries.getConsentGroupUser(user, group), lastModified)) {
            case Success(groupUser) => subRoute(groupUser)
            case Failure(t) => throw t
          }
        }
      }
    }
  }

  def askConsentGroup(command: ConsentGroupCommand): Route = askUserRegistration(command)
}
