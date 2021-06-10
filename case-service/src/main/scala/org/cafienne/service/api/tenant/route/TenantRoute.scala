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
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.service.api.Headers
import org.cafienne.service.api.tenant.TenantReader
import org.cafienne.service.api.tenant.model.TenantAPI.{BackwardsCompatibleTenantFormat, UserFormat}
import org.cafienne.tenant.actorapi.command.{TenantCommand, TenantUserInformation}
import org.cafienne.tenant.actorapi.command.platform.{CreateTenant, PlatformTenantCommand}

trait TenantRoute extends CommandRoute with QueryRoute {

  override val lastModifiedRegistration = TenantReader.lastModifiedRegistration

  override val lastModifiedHeaderName: String = Headers.TENANT_LAST_MODIFIED

  def askPlatform(command: PlatformTenantCommand) = {
    askModelActor(command)
  }

  def askTenant(platformUser: PlatformUser, tenant: String, createTenantCommand: CreateTenantCommand) = {
    askModelActor(createTenantCommand.apply(platformUser.getTenantUser(tenant)))
  }

  trait CreateTenantCommand {
    def apply(tenantUser: TenantUser): TenantCommand
  }

  def invokeCreateTenant(platformOwner: PlatformUser, newTenant: BackwardsCompatibleTenantFormat) = {
    import scala.collection.JavaConverters._

    val users = convertToTenant(newTenant).asJava
    if (users.isEmpty) {
      complete(StatusCodes.BadRequest, "Creation of tenant cannot be done without users and at least one owner")
    } else {
      val newTenantName = newTenant.name
      askPlatform(new CreateTenant(platformOwner, newTenantName, newTenantName, users))
    }
  }

  def convertToTenant(tenant: BackwardsCompatibleTenantFormat): Seq[TenantUserInformation] = {
    val users = tenant.users.getOrElse(tenant.owners.getOrElse(Seq()))
    val defaultOwnership: Option[Boolean] = {
      if (tenant.users.isEmpty && tenant.owners.nonEmpty) Some(true) // Owners is the old format, then all users become owner.
      else None // In the new format every owner must be explicitly defined
    }
    users.map(user => asTenantUser(user, tenant.name, defaultOwnership))
  }

  def asTenantUser(user: UserFormat, tenant: String, defaultOwnership: Option[Boolean] = None): TenantUserInformation = {
    val ownerShip = defaultOwnership.fold(user.isOwner)(Some(_))
    TenantUserInformation(user.userId, roles = user.roles, name = user.name, email = user.email, owner = ownerShip, enabled = user.enabled)
  }
}

