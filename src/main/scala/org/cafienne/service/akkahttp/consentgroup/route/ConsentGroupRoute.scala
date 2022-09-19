/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
