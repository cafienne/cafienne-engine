package org.cafienne.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.{CaseTables, TaskTables}
import slick.migration.api.{SqlMigration, TableMigration}

trait CafienneTablesV2 extends CafienneTablesV3 {

  import dbConfig.profile.api._

  case class TenantOwnerRecord(tenant: String, userId: String, enabled: Boolean = true)
  case class CaseTeamMemberRecord(caseInstanceId: String, tenant: String, memberId: String, caseRole: String, isTenantUser: Boolean, isOwner: Boolean, active: Boolean)

  class CaseInstanceTeamMemberTable(tag: Tag) extends CafienneTenantTable[CaseTeamMemberRecord](tag, "case_instance_team_member") {

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val caseRole = idColumn[String]("case_role")

    lazy val memberId = userColumn[String]("member_id")

    lazy val isTenantUser = column[Boolean]("isTenantUser")

    lazy val isOwner = column[Boolean]("isOwner")

    lazy val active = column[Boolean]("active")

    lazy val pk = primaryKey(pkName, (caseInstanceId, caseRole, memberId, isTenantUser))

    lazy val * = (caseInstanceId, tenant, memberId, caseRole, isTenantUser, isOwner, active).mapTo[CaseTeamMemberRecord]

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    lazy val indexMemberId = index(oldStyleIxName(memberId), (memberId, isTenantUser))
  }

  // Schema for the "tenant-owner" table:
  final class TenantOwnersTable(tag: Tag) extends CafienneTenantTable[TenantOwnerRecord](tag, "tenant_owners") {

    lazy val * = (tenant, userId, enabled).mapTo[TenantOwnerRecord]

    lazy val enabled = column[Boolean]("enabled", O.Default(true))

    lazy val pk = primaryKey(pkName, (tenant, userId))

    lazy val userId = userColumn[String]("userId")
  }
}


object QueryDB_1_1_6 extends DbSchemaVersion with QueryDBSchema
  with CaseTables
  with TaskTables
  with CafienneTablesV1 {

  val version = "1.1.6"
  val migrations = (
    renameCaseTableDefinitionColumn &

    // We need to change CaseTeam table to also have a column for member type, which is also part of the primary key
    dropCaseTeamPK & enhanceCaseTeamTable & addUpdatedCaseTeamPK &

    // Now replace all foreign keys with indexes
    convertFKtoIndexPlanItemTable &
    convertFKtoIndexCaseTeamTable &
    convertFKtoIndexCaseFileTable &
    convertFKtoIndexCaseRolesTable &

    // Add various indexes to improve performance of searching tasks
    addTaskTableIndices &

    // Add ownership field to user role table for faster and simpler querying
    addUserRoleOwnerColumn & resetTenantProjection & dropTenantOwnersTable &

    // Add a new table to store business identifiers
    addBusinessIdentifierTable
  )

  import dbConfig.profile.api._

  def renameCaseTableDefinitionColumn = TableMigration(TableQuery[CaseInstanceTable])
    .renameColumnFrom("definition", _.caseName)
    .addIndexes(_.indexCaseName, _.indexTenant, _.indexRootCaseId, _.indexState)

  def dropCaseTeamPK = TableMigration(TableQuery[CaseInstanceTeamMemberTableV1]).dropPrimaryKeys(_.pk_V1)

  // Add 2 new columns for memberType ("user" or "role") and case ownership
  //  Existing members all get memberType "user" and also all of them get ownership.
  //  Ownership is needed, because otherwise no one can change the case team anymore...
  // Also we rename columns role and user_id to caseRole and memberId (since member is not just user but can also hold a tenant role)
  def enhanceCaseTeamTable = TableMigration(TableQuery[CaseInstanceTeamMemberTable])
    .renameColumnFrom("user_id", _.memberId)
    .renameColumnFrom("role", _.caseRole)
    .addColumnAndSet(_.isTenantUser, true)
    .addColumnAndSet(_.isOwner, true)

  def addUpdatedCaseTeamPK = TableMigration(TableQuery[CaseInstanceTeamMemberTable]).addPrimaryKeys(_.pk)

  def convertFKtoIndexPlanItemTable = TableMigration(TableQuery[PlanItemTableV1]).addIndexes(_.indexCaseInstanceId).dropForeignKeys(_.fkCaseInstanceTable)
  def convertFKtoIndexCaseTeamTable = TableMigration(TableQuery[CaseInstanceTeamMemberTableV1]).addIndexes(_.indexCaseInstanceId).dropForeignKeys(_.fkCaseInstanceTable)
  def convertFKtoIndexCaseRolesTable = TableMigration(TableQuery[CaseInstanceRoleTableV1]).addIndexes(_.indexCaseInstanceId).dropForeignKeys(_.fkCaseInstanceTable)
  def convertFKtoIndexCaseFileTable = TableMigration(TableQuery[CaseFileTableV1]).addIndexes(_.indexCaseInstanceId).dropForeignKeys(_.fkCaseInstanceTable)

  def addTaskTableIndices = TableMigration(TableQuery[TaskTable]).addIndexes(_.indexAssignee, _.indexCaseInstanceId, _.indexDueDate, _.indexTaskState, _.indexTenant)

  def addUserRoleOwnerColumn = TableMigration(TableQuery[UserRoleTable]).addColumns(_.isOwner)

  def resetTenantProjection = SqlMigration(s"""DELETE FROM "offset_storage" where "name" = 'TenantProjectionsWriter' """)

  def dropTenantOwnersTable = TableMigration(TableQuery[TenantOwnersTable]).drop

  def addBusinessIdentifierTable = TableMigration(TableQuery[CaseBusinessIdentifierTable])
    .create
    .addColumns(
      _.caseInstanceId,
      _.tenant,
      _.name,
      _.value,
      _.active,
      _.path
    )
    .addPrimaryKeys(_.pk)
    .addIndexes(_.indexCaseInstanceId, _.indexName)
}
