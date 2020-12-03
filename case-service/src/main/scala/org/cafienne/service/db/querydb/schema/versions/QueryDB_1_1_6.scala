package org.cafienne.service.db.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.service.api.projection.table.{CaseTables, TaskTables}
import org.cafienne.service.db.querydb.QueryDBSchema
import org.cafienne.service.db.querydb.schema.Projections
import slick.migration.api.TableMigration

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
    addUserRoleOwnerColumn & Projections.resetTenantProjectionWriter & dropTenantOwnersTable &

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
