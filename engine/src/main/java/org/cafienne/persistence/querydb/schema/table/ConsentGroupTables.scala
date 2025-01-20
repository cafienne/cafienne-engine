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

import org.cafienne.persistence.querydb.record.{ConsentGroupMemberRecord, ConsentGroupRecord}
import org.cafienne.persistence.querydb.schema.QueryDBSchema

trait ConsentGroupTables extends QueryDBSchema {

  import dbConfig.profile.api._

  // Schema for the "consentgroup" table:
  final class ConsentGroupTable(tag: Tag) extends CafienneTable[ConsentGroupRecord](tag, "consentgroup") {
    // Columns
    lazy val id = idColumn[String]("id", O.PrimaryKey)
    lazy val tenant = idColumn[String]("tenant")

    // Constraints
    lazy val pk = primaryKey(pkName, id)
    lazy val * = (id, tenant).mapTo[ConsentGroupRecord]
  }

  class ConsentGroupMemberTable(tag: Tag) extends CafienneTable[ConsentGroupMemberRecord](tag, "consentgroup_member") {
    // Columns
    lazy val group = idColumn[String]("group")
    lazy val userId = userColumn[String]("user_id")
    lazy val role = idColumn[String]("role")
    lazy val isOwner = column[Boolean]("isOwner", O.Default(false))

    // Constraints
    lazy val pk = primaryKey(pkName, (userId, group, role))
    lazy val indexOwnership = index(ixName(isOwner), (group, userId, role, isOwner))

    lazy val * = (group, userId, role, isOwner).mapTo[ConsentGroupMemberRecord]
  }
}