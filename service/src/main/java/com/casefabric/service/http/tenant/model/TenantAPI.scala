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

package com.casefabric.service.http.tenant.model

import io.swagger.v3.oas.annotations.media.Schema
import com.casefabric.actormodel.identity.TenantUser
import com.casefabric.service.infrastructure.payload.EntityReader.{EntityReader, entityReader}
import com.casefabric.service.infrastructure.payload.PayloadValidator

import scala.annotation.meta.field

object TenantAPI {

  implicit val userReader: EntityReader[UserFormat] = entityReader[UserFormat]
  implicit val tenantReader: EntityReader[TenantFormat] = entityReader[TenantFormat]
  implicit val replaceTenantReader: EntityReader[ReplaceTenantFormat] = entityReader[ReplaceTenantFormat]

  case class UserFormat(
                         @(Schema @field)(implementation = classOf[String], example = "User id (matched with token when user logs on)") userId: String,
                         @(Schema @field)(description = "Option to set the list of roles the user has within the tenant") roles: Set[String] = Set(),
                         @(Schema @field)(example = "Option to indicate tenant ownership changes for this user (defaults to false)", implementation = classOf[Boolean]) isOwner: Boolean = false,
                         @(Schema @field)(example = "Option to change the user name", implementation = classOf[String]) name: String = "",
                         @(Schema @field)(example = "Option to change the user email", implementation = classOf[String]) email: String = "",
                         @(Schema @field)(example = "Option to indicate whether account must be enabled/disabled (defaults to true)", implementation = classOf[Boolean]) enabled: Boolean = true) {
    PayloadValidator.required(userId, "Tenant users must have a userId")
    PayloadValidator.requireNonNullElements(roles.toSeq, "Roles cannot be null")
    def asTenantUser(tenant: String): TenantUser = {
      TenantUser(id = userId, tenant = tenant, roles = roles, isOwner = isOwner, name = name, email = email, enabled = enabled)
    }
  }

  private def validateUserList(users: Seq[UserFormat]): Unit = {
    PayloadValidator.requireElements(users, "Setting tenant requires a list of users with at least one owner")
    PayloadValidator.runDuplicatesDetector("Tenant", "user", users.map(_.userId))
  }


  case class TenantFormat(name: String, users: Seq[UserFormat] = Seq()) {
    validateUserList(users)
    def getTenantUsers: Seq[TenantUser] = users.map(_.asTenantUser(name))
  }

  case class ReplaceTenantFormat(users: Seq[UserFormat] = Seq(), name: String = "") {
    validateUserList(users)

    def getTenantUsers(tenant: String): Seq[TenantUser] = users.map(_.asTenantUser(tenant))
  }

  case class TenantUserResponseFormat(
    @(Schema @field)(implementation = classOf[String], example = "Same as platform user id") userId: String,
    @(Schema @field)(example = "Tenant name", implementation = classOf[String]) tenant: String,
    @(Schema @field)(description = "List of roles the user has within the tenant") roles: Set[String],
    @(Schema @field)(example = "Optional user name", implementation = classOf[String]) name: String,
    @(Schema @field)(example = "Optional user email", implementation = classOf[String]) email: String,
    @(Schema @field)(example = "Whether user is tenant owner", implementation = classOf[String]) isOwner: Boolean) {
  }

  case class PlatformUserFormat(userId: String, tenants: Seq[TenantUserResponseFormat])
}
