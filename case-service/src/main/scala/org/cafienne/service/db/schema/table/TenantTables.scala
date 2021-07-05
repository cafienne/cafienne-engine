package org.cafienne.service.db.schema.table

import org.cafienne.service.db.record.{TenantOwnerRecord, TenantRecord, UserRoleRecord}
import org.cafienne.service.db.schema.QueryDBSchema

trait TenantTables extends QueryDBSchema {

  import dbConfig.profile.api._

  // Schema for the "tenant-owner" table:
  final class TenantOwnersTable(tag: Tag) extends CafienneTenantTable[TenantOwnerRecord](tag, "tenant_owners") {

    def * = (tenant, userId, enabled).mapTo[TenantOwnerRecord]

    def enabled = column[Boolean]("enabled", O.Default(true))

    def pk = primaryKey("pk_tenant_owner", (tenant, userId))

    def userId = userColumn[String]("userId")
  }

  // Schema for the "tenant" table:
  final class TenantTable(tag: Tag) extends CafienneTable[TenantRecord](tag, "tenant") {

    // Columsn
    def name = idColumn[String]("name", O.PrimaryKey)
    def enabled = column[Boolean]("enabled", O.Default(true))

    // Constraints
    def pk = primaryKey("pk_tenant", name)
    def * = (name, enabled).mapTo[TenantRecord]
  }

  class UserRoleTable(tag: Tag) extends CafienneTenantTable[UserRoleRecord](tag, "user_role") {
    def pk = primaryKey("pk_userrole", (userId, tenant, role_name))

    def * = (userId, tenant, role_name, name, email, isOwner, enabled).mapTo[UserRoleRecord]

    def userId = userColumn[String]("user_id")

    def role_name = idColumn[String]("role_name")

    def name = column[String]("name")

    def email = column[String]("email")

    def isOwner = column[Boolean]("isOwner", O.Default(false))

    // By default when inserting a user or role, enabled is true;
    //  right now, it does not make sense to enter a user without enabling it
    def enabled = column[Boolean]("enabled", O.Default(true))

    def indexOwnership = index(generateIndexName(isOwner), (userId, tenant, role_name, isOwner))
  }

}