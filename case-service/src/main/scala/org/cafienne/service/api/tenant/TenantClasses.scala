package org.cafienne.service.api.tenant

final case class UserRole(userId: String, tenant: String, role_name: String, name: String, email: String = "", enabled: Boolean = true)

final case class User(id: String, tenant: String, name: String, email: String = "", enabled: Boolean = true)

final case class Tenant(name: String, enabled: Boolean = true)

final case class TenantOwner(tenant: String, userId: String, enabled: Boolean = true)
