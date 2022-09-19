/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tenant.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.querydb.materializer.LastModifiedRegistration
import org.cafienne.querydb.materializer.tenant.TenantReader
import org.cafienne.querydb.query.{TenantQueriesImpl, UserQueries}
import org.cafienne.service.akkahttp.Headers
import org.cafienne.tenant.actorapi.command.TenantCommand

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait TenantRoute extends CommandRoute with QueryRoute {
  val userQueries: UserQueries = new TenantQueriesImpl

  override val lastModifiedRegistration: LastModifiedRegistration = TenantReader.lastModifiedRegistration

  override val lastModifiedHeaderName: String = Headers.TENANT_LAST_MODIFIED

  def tenantUser(subRoute: TenantUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { group =>
        optionalHeaderValueByName(Headers.TENANT_LAST_MODIFIED) { lastModified =>
          onComplete(getTenantUser(user, group, lastModified)) {
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

  def getTenantUser(user: AuthenticatedUser, tenant: String, lastModified: Option[String]): Future[TenantUser] = {
    runSyncedQuery(userQueries.getTenantUser(user, tenant), lastModified)
  }

  def askTenant(command: TenantCommand): Route = {
    askModelActor(command)
  }
}
