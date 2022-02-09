package org.cafienne.querydb.schema.table

import org.cafienne.querydb.record.{ConsentGroupMemberRecord, ConsentGroupRecord}
import org.cafienne.querydb.schema.QueryDBSchema

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