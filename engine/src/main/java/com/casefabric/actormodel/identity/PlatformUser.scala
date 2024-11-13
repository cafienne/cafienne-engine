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

package com.casefabric.actormodel.identity

import com.casefabric.actormodel.exception.{AuthorizationException, MissingTenantException}
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.infrastructure.serialization.Fields
import com.casefabric.json.{CaseFabricJson, Value, ValueMap}

final case class PlatformUser(id: String, users: Seq[TenantUser], groups: Seq[ConsentGroupMembership] = Seq()) extends UserIdentity {
  def tenants: Seq[String] = users.map(u => u.tenant)

  def origin(tenant: String): Origin = {
    if (users.isEmpty && groups.isEmpty) {
      // No tenant users. Means trust level is IDP only.
      Origin.IDP
    } else if (isTenantMember(tenant)) {
      // User is a tenant member, so trust level is Tenant itself
      Origin.Tenant
    } else {
      // User is known in the platform, but not part of the tenant
      Origin.Platform
    }
  }

  def tenantRoles(tenant: String): Set[String] = {
    if (isTenantMember(tenant)) getTenantUser(tenant).roles
    else Set()
  }

  def isGroupMember(groupId: String): Boolean = groups.exists(_.groupId == groupId)

  def group(groupId: String): ConsentGroupMembership = groups.find(_.groupId == groupId).orNull

  /**
    * If the user is registered in one tenant only, that tenant is returned.
    * Otherwise, the default tenant of the platform is returned, but it fails when the user is not a member of that tenant
    *
    * @return
    */
  def defaultTenant: String = {
    if (tenants.length == 1) {
      tenants.head
    } else {
      val configuredDefaultTenant = CaseFabric.config.platform.defaultTenant
      if (configuredDefaultTenant.isEmpty) {
        throw new MissingTenantException("Tenant property must have a value")
      }
      if (!tenants.contains(configuredDefaultTenant)) {
        if (tenants.isEmpty) {
          // Throws an exception that user does not belong to any tenant
          getTenantUser("")
        }
        throw new MissingTenantException("Tenant property must have a value, because user belongs to multiple tenants")
      }
      configuredDefaultTenant
    }
  }

  def resolveTenant(optionalTenant: Option[String]): String = {
    optionalTenant match {
      case None => defaultTenant // This will throw an IllegalArgumentException if the default tenant is not configured
      case Some(tenant) => if (tenant.isBlank) {
        defaultTenant
      } else {
        tenant
      }
    }
  }

  override def toValue: Value[_] = {
    new ValueMap(Fields.userId, id, Fields.tenants, users, Fields.groups, groups)
  }

  def shouldBelongTo(tenant: String): Unit = if (!isTenantMember(tenant)) throw AuthorizationException("Tenant '" + tenant + "' does not exist, or user '" + id + "' is not registered in it")

  def isTenantMember(tenant: String): Boolean = users.find(_.tenant == tenant).fold(false)(_.enabled)

  def isPlatformOwner: Boolean = CaseFabric.isPlatformOwner(id)

  def getTenantUser(tenant: String): TenantUser = users.find(u => u.tenant == tenant).getOrElse({
    val message = tenants.isEmpty match {
      case true => s"User '$id' is not registered in a tenant"
      case false => s"User '$id' is not registered in tenant '$tenant'; user is registered in ${tenants.size} other tenant(s) "
    }
    throw AuthorizationException(message)
  })
}

object PlatformUser {
  def from(user: UserIdentity) = new PlatformUser(user.id, Seq())
}

case class ConsentGroupMembership(groupId: String, roles: Set[String], isOwner: Boolean) extends CaseFabricJson {
  override def toValue: Value[_] = {
    val json = new ValueMap(Fields.groupId, groupId, Fields.isOwner, isOwner, Fields.roles, roles)
    json
  }
}

object ConsentGroupMembership {
  def deserialize(json: ValueMap): ConsentGroupMembership = {
    val groupId: String = json.readString(Fields.groupId)
    val isOwner: Boolean = json.readBoolean(Fields.isOwner)
    val roles = json.readStringList(Fields.roles).toSet

    ConsentGroupMembership(groupId = groupId, roles = roles, isOwner = isOwner)
  }
}
