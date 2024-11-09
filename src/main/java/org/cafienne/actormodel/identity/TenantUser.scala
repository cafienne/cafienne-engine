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

package org.cafienne.actormodel.identity

import com.fasterxml.jackson.core.JsonGenerator
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}
import org.cafienne.tenant.actorapi.event.deprecated._

import java.util

final case class TenantUser(id: String, tenant: String, override val roles: Set[String] = Set(), isOwner: Boolean = false, name: String = "", email: String = "", enabled: Boolean = true) extends UserIdentity {

  import scala.jdk.CollectionConverters._

  override def asCaseUserIdentity(): CaseUserIdentity = CaseUserIdentity(id, Origin.Tenant)

  def getRoles: util.Set[String] = roles.asJava

  def differs(that: TenantUser): Boolean = {
    def differentRoles(that: TenantUser): Boolean = that.roles.exists(role => !this.roles.contains(role)) || this.roles.exists(role => !that.roles.contains(role))

    this.id != that.id ||
      this.tenant != that.tenant ||
      this.isOwner != that.isOwner ||
      this.name != that.name ||
      this.email != that.email ||
      this.enabled != that.enabled ||
      differentRoles(that)
  }

  /**
    * Serializes the user information to JSON
    *
    * @param generator
    */
  override def write(generator: JsonGenerator): Unit = {
    generator.writeStartObject()
    writeField(generator, Fields.userId, id)
    writeField(generator, Fields.roles, roles.asJava)
    writeField(generator, Fields.tenant, tenant)
    writeField(generator, Fields.name, name)
    writeField(generator, Fields.email, email)
    writeField(generator, Fields.isOwner, isOwner)
    writeField(generator, Fields.enabled, enabled)
    generator.writeEndObject()
  }

  override def toValue: Value[_] = new ValueMap(
    Fields.userId, id,
    Fields.roles, roles.toArray,
    Fields.tenant, tenant,
    Fields.name, name,
    Fields.email, email,
    Fields.isOwner, isOwner,
    Fields.enabled, enabled)
}

object TenantUser extends LazyLogging {
  /**
    * Deserialize the json into a user context
    *
    * @param json
    * @return instance of user context
    */
  def deserialize(json: ValueMap): TenantUser = {
    val id: String = json.readString(Fields.userId)
    val name: String = json.readString(Fields.name, "")
    val email: String = json.readString(Fields.email, "")
    val tenant: String = json.readString(Fields.tenant)
    val isOwner: Boolean = json.readBoolean(Fields.isOwner)
    val enabled: Boolean = json.readBoolean(Fields.enabled)
    val roles = json.readStringList(Fields.roles).toSet

    TenantUser(id = id, tenant = tenant, roles = roles, isOwner = isOwner, name = name, email = email, enabled = enabled)
  }

  def handleDeprecatedEvent(users: util.Map[String, TenantUser], event: DeprecatedTenantUserEvent): Unit = {
    val userId = event.userId

    val user: TenantUser = event match {
      case event: TenantUserCreated => new TenantUser(id = event.userId, tenant = event.tenant, roles = Set(), name = event.name, email = event.email, isOwner = false, enabled = true)
      case _ =>
        val user = users.get(event.userId)
        if (user == null) {
          // Now what....
          logger.error("Ignoring event of type " + getClass.getName + ", because user with id " + userId + " does not exist in tenant " + event.tenant)
          null
        } else {
          event match {
            case t: TenantUserUpdated => user.copy(name = t.name, email = t.email)
            case r: TenantUserRoleAdded => user.copy(roles = user.roles ++ Set(r.role))
            case r: TenantUserRoleRemoved => user.copy(roles = user.roles -- Set(r.role))
            case _: OwnerAdded => user.copy(isOwner = true)
            case _: OwnerRemoved => user.copy(isOwner = false)
            case _: TenantUserDisabled => user.copy(enabled = false)
            case _: TenantUserEnabled => user.copy(enabled = true)
            case _ => null
          }
        }
    }
    if (user != null) { // It can happen that there is no user
      users.put(userId, user)
    }
  }
}
