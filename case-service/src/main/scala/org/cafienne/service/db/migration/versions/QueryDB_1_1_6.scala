package org.cafienne.service.db.migration.versions

import org.cafienne.service.api.cases.table.CaseTables
import org.cafienne.service.api.tasks.TaskTables
import org.cafienne.service.db.migration.{DbSchemaVersion, Projections}
import slick.migration.api.TableMigration

object QueryDB_1_1_6 extends DbSchemaVersion
  with CaseTables
  with TaskTables
  with CafienneTablesV1 {

  val version = "1.1.6"
  val migrations = (
    // We need to change CaseTeam table to also have a column for member type, which is also part of the primary key
    dropCaseTeamPK & enhanceCaseTeamTable & addUpdatedCaseTeamPK &

    // We also need a task team table; but this requires that Task projection is rebuilt
    createTaskTeamTable & Projections.resetTaskProjectionWriter &

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

  def createTaskTeamTable = TableMigration(TableQuery[TaskTeamMemberTable])
    .create
    .addColumns(
      _.memberId,
      _.caseInstanceId,
      _.tenant,
      _.caseRole,
      _.isTenantUser,
      _.isOwner,
      _.active
    )
    .addPrimaryKeys(_.pk)

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
