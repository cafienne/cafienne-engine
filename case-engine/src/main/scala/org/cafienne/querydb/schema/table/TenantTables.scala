package org.cafienne.querydb.schema.table

import org.cafienne.querydb.record.{TenantRecord, UserRoleRecord}
import org.cafienne.querydb.schema.QueryDBSchema

trait TenantTables extends QueryDBSchema {

  import dbConfig.profile.api._

  // Schema for the "tenant" table:
  final class TenantTable(tag: Tag) extends CafienneTable[TenantRecord](tag, "tenant") {

    // Columsn
    lazy val name = idColumn[String]("name", O.PrimaryKey)
    lazy val enabled = column[Boolean]("enabled", O.Default(true))

    // Constraints
    lazy val pk = primaryKey(pkName, name)
    lazy val * = (name, enabled).mapTo[TenantRecord]
  }

  class UserRoleTable(tag: Tag) extends CafienneTenantTable[UserRoleRecord](tag, "user_role") {
    lazy val pk = primaryKey("pk_userrole", (userId, tenant, role_name)) // Note: cannot use pkName becauase pk_userrole deviates from the actual table name :(

    lazy val * = (userId, tenant, role_name, name, email, isOwner, enabled).mapTo[UserRoleRecord]

    lazy val userId = userColumn[String]("user_id")

    lazy val role_name = idColumn[String]("role_name")

    lazy val name = column[String]("name")

    lazy val email = column[String]("email")

    lazy val isOwner = column[Boolean]("isOwner", O.Default(false))

    // By default when inserting a user or role, enabled is true;
    //  right now, it does not make sense to enter a user without enabling it
    lazy val enabled = column[Boolean]("enabled", O.Default(true))

    lazy val indexOwnership = index(oldStyleIxName(isOwner), (userId, tenant, role_name, isOwner))
  }

}