package org.cafienne.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.{CaseTables, TaskTables, TenantTables}
import slick.migration.api.TableMigration

object QueryDB_1_1_11 extends DbSchemaVersion with QueryDBSchema
  with CaseTables
  with TaskTables
  with TenantTables
  with CafienneTablesV2 {

  val version = "1.1.11"
  val migrations = (
    // These indices are required for faster updating platform user id's
    //  Probably also some queries on tasks may become faster ...
    addPlanItemIndices & addPlanItemHistoryIndex & addCaseIndices & addCaseTeamIndex & addTaskIndices

      // This index is needed to optimize tenant queries
      & addUserRoleIndex
  )

  import dbConfig.profile.api._

  def addPlanItemIndices = TableMigration(TableQuery[PlanItemTable]).addIndexes(_.indexCreatedBy, _.indexModifiedBy)
  def addPlanItemHistoryIndex = TableMigration(TableQuery[PlanItemHistoryTable]).addIndexes(_.indexModifiedBy)
  def addCaseIndices = TableMigration(TableQuery[CaseInstanceTable]).addIndexes(_.indexCreatedBy, _.indexModifiedBy)
  def addCaseTeamIndex = TableMigration(TableQuery[CaseInstanceTeamMemberTable]).addIndexes(_.indexMemberId)
  def addTaskIndices = TableMigration(TableQuery[TaskTable]).addIndexes(_.indexOwner, _.indexCreatedBy, _.indexModifiedBy)
  def addUserRoleIndex = TableMigration(TableQuery[UserRoleTable]).addIndexes(_.indexOwnership)

}
