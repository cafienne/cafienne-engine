package org.cafienne.service.api.participants

import org.cafienne.infrastructure.jdbc.DbConfig

final case class UserRole(userId: String, tenant: String, role_name: String, name: String, email: String = "", enabled: Boolean = true)

final case class User(id: String, tenant: String, name: String, email: String = "", enabled: Boolean = true)

final case class Tenant(name: String, enabled: Boolean = true)

final case class TenantOwner(tenant: String, userId: String, enabled: Boolean = true)

trait UserTables extends DbConfig {

  import dbConfig.profile.api._

  // Schema for the "tenant-owner" table:
  final class TenantOwnersTable(tag: Tag) extends Table[TenantOwner](tag, "tenant_owners") {

    def * = (tenant, userId, enabled) <> (TenantOwner.tupled, TenantOwner.unapply)

    def enabled = column[Boolean]("enabled", O.Default(true))

    def pk = primaryKey("pk_tenant_owner", (tenant, userId))

    def tenant = column[String]("tenant")

    def userId = column[String]("userId")
  }

  // Schema for the "tenant" table:
  final class TenantTable(tag: Tag) extends Table[Tenant](tag, "tenant") {

    def * = (name, enabled) <> (Tenant.tupled, Tenant.unapply)

    def name = column[String]("name", O.PrimaryKey)

    def enabled = column[Boolean]("enabled", O.Default(true))
  }

  final class UserRoleTable(tag: Tag) extends Table[UserRole](tag, "user_role") {
    def pk = primaryKey("pk_userrole", (userId, tenant, role_name))

    def * = (userId, tenant, role_name, name, email, enabled) <> (UserRole.tupled, UserRole.unapply)

    def userId = column[String]("user_id")

    def tenant = column[String]("tenant")

    def role_name = column[String]("role_name")

    def name = column[String]("name")

    def email = column[String]("email")

    // By default when inserting a user or role, enabled is true;
    //  right now, it does not make sense to enter a user without enabling it
    def enabled = column[Boolean]("enabled", O.Default(true))
  }

}