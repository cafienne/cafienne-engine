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
import org.cafienne.service.api
import org.cafienne.service.api.tenant.TenantReader
import org.cafienne.service.api.tenant.model.TenantAPI.{BackwardsCompatibleTenantFormat, UserFormat}
import org.cafienne.tenant.akka.command.TenantCommand
import org.cafienne.tenant.akka.command.platform.{CreateTenant, PlatformTenantCommand}

trait TenantRoute extends CommandRoute with QueryRoute {

  override val lastModifiedRegistration = TenantReader.lastModifiedRegistration

  override val lastModifiedHeaderName: String = api.TENANT_LAST_MODIFIED

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

  def convertToTenant(tenant: BackwardsCompatibleTenantFormat): Seq[TenantUser] = {
    val users = tenant.users.getOrElse(tenant.owners.getOrElse(Seq()))
    val defaultOwnership: Boolean = {
      if (tenant.users.isEmpty && tenant.owners.nonEmpty) true // Owners is the old format, then all users become owner.
      else false // In the new format every owner must be explicitly defined
    }
    users.map(user => asTenantUser(user, tenant.name, defaultOwnership))
  }

  def asTenantUser(user: UserFormat, tenant: String, defaultOwnership: Boolean = false): TenantUser = {
    TenantUser(user.userId, user.roles, tenant, isOwner = user.isOwner.getOrElse(defaultOwnership), user.name.getOrElse(""), user.email.getOrElse(""), enabled = true)
  }
}

