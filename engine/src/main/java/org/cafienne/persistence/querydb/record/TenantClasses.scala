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

package org.cafienne.persistence.querydb.record

import org.cafienne.tenant.actorapi.event.deprecated.{DeprecatedTenantUserEvent, TenantUserRoleEvent}

final case class UserRoleRecord(userId: String, tenant: String, role_name: String, name: String, email: String, isOwner: Boolean, enabled: Boolean) {
  val key = UserRoleKey(userId, tenant, role_name)
}

final case class UserRoleKey(userId: String, tenant: String, role_name: String)

final case class TenantRecord(name: String, enabled: Boolean = true)

object UserRoleKey {
  def apply(event: DeprecatedTenantUserEvent): UserRoleKey = event match {
    case event: TenantUserRoleEvent => UserRoleKey(event.userId, event.tenant, event.role)
    case event: DeprecatedTenantUserEvent => UserRoleKey(event.userId, event.tenant, "")
  }
}