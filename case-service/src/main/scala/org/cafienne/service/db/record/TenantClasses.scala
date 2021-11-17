package org.cafienne.service.db.record

import org.cafienne.tenant.actorapi.event.{TenantUserEvent, TenantUserRoleEvent}

final case class UserRoleRecord(userId: String, tenant: String, role_name: String, name: String, email: String, isOwner: Boolean, enabled: Boolean) {
  val key = UserRoleKey(userId, tenant, role_name)
}

final case class UserRoleKey(userId: String, tenant: String, role_name: String)

final case class TenantRecord(name: String, enabled: Boolean = true)

object UserRoleKey {
  def apply(event: TenantUserEvent): UserRoleKey = event match {
    case event: TenantUserRoleEvent => UserRoleKey(event.userId, event.tenant, event.role)
    case event: TenantUserEvent => UserRoleKey(event.userId, event.tenant, "")
  }
}