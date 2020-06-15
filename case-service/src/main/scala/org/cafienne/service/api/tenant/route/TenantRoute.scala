/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.tenant.akka.command.TenantCommand
import org.cafienne.tenant.akka.command.platform.PlatformTenantCommand

import scala.concurrent.Future

trait TenantRoute extends CommandRoute with QueryRoute {
  override val lastModifiedRegistration = null
  override def handleSyncedQuery[A](query: () => Future[A], clm: Option[String]): Future[A] = {
    query()
  }


  def askPlatform(command: PlatformTenantCommand) = {
    askModelActor(command)
  }

  def askTenant(platformUser: PlatformUser, tenant: String, createTenantCommand: CreateTenantCommand) = {
    askModelActor(createTenantCommand.apply(platformUser.getTenantUser(tenant)))
  }

  trait CreateTenantCommand {
    def apply(tenantUser: TenantUser): TenantCommand
  }
}

