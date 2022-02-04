/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.consentgroup.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{PlatformUser, TenantUser}
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.service.akkahttp.Headers
import org.cafienne.service.db.materializer.LastModifiedRegistration
import org.cafienne.service.db.materializer.consentgroup.ConsentGroupReader
import org.cafienne.service.db.query.UserQueries
import org.cafienne.service.db.query.exception.ConsentGroupSearchFailure

import scala.util.{Failure, Success}

trait ConsentGroupRoute extends CommandRoute with QueryRoute {
  override val lastModifiedRegistration: LastModifiedRegistration = ConsentGroupReader.lastModifiedRegistration
  override val lastModifiedHeaderName: String = Headers.CONSENT_GROUP_LAST_MODIFIED
  val userQueries: UserQueries

  def askConsentGroup(platformUser: PlatformUser, group: String, command: TenantUser => ConsentGroupCommand): Route = {
    onComplete(userQueries.authorizeConsentGroupMembershipAndReturnTenant(platformUser, group)) {
      case Success(tenant) => askModelActor(command(platformUser.getTenantUser(tenant)))
      case Failure(error) =>
        error match {
          case t: ConsentGroupSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
          case _ => throw error
        }
    }
  }
}
