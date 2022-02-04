/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{PlatformOwner, PlatformUser, TenantUser}
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.service.api.Headers
import org.cafienne.service.db.materializer.LastModifiedRegistration
import org.cafienne.service.db.materializer.tenant.TenantReader
import org.cafienne.tenant.actorapi.command.TenantCommand
import org.cafienne.tenant.actorapi.command.platform.PlatformTenantCommand

trait TenantRoute extends CommandRoute with QueryRoute {

  override val lastModifiedRegistration: LastModifiedRegistration = TenantReader.lastModifiedRegistration

  override val lastModifiedHeaderName: String = Headers.TENANT_LAST_MODIFIED

  def validPlatformOwner(subRoute: PlatformOwner => Route): Route = {
    validUser { platformUser =>
      if (platformUser.isPlatformOwner) {
        subRoute(PlatformOwner(platformUser.id))
      } else {
        complete(StatusCodes.Unauthorized, "Only platform owners can access this route")
      }
    }
  }

  def askPlatform(command: PlatformTenantCommand): Route = {
    askModelActor(command)
  }

  def askTenant(platformUser: PlatformUser, tenant: String, createTenantCommand: TenantUser => TenantCommand): Route = {
    askModelActor(createTenantCommand.apply(platformUser.getTenantUser(tenant)))
  }

}

