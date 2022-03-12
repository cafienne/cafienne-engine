/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tenant.route

import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{PlatformOwner, PlatformUser, TenantUser}
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.querydb.materializer.LastModifiedRegistration
import org.cafienne.querydb.materializer.tenant.TenantReader
import org.cafienne.service.akkahttp.Headers
import org.cafienne.tenant.actorapi.command.TenantCommand

trait TenantRoute extends CommandRoute with QueryRoute {

  override val lastModifiedRegistration: LastModifiedRegistration = TenantReader.lastModifiedRegistration

  override val lastModifiedHeaderName: String = Headers.TENANT_LAST_MODIFIED

  def askTenant(platformUser: PlatformUser, tenant: String, createTenantCommand: TenantUser => TenantCommand): Route = {
    askModelActor(createTenantCommand.apply(platformUser.getTenantUser(tenant)))
  }
}

