package org.cafienne.service.api.tenant

final case class UserRoleRecord(userId: String, tenant: String, role_name: String, name: String, email: String = "", enabled: Boolean = true)

final case class User(id: String, tenant: String, name: String, email: String = "", enabled: Boolean = true)

final case class TenantRecord(name: String, enabled: Boolean = true)

final case class TenantOwnerRecord(tenant: String, userId: String, enabled: Boolean = true)
