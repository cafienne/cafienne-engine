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

package org.cafienne.persistence.querydb.schema.table

import org.cafienne.persistence.querydb.record.{TenantRecord, UserRoleRecord}
import org.cafienne.persistence.querydb.schema.QueryDBSchema

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

    lazy val indexUserRoleTenant = index("ix_user_role__userId_role_name_tenant", (userId, role_name, tenant))

    lazy val indexOwnership = index(oldStyleIxName(isOwner), (userId, tenant, role_name, isOwner))
  }

}