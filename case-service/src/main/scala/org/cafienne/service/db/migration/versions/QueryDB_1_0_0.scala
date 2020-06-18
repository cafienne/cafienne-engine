package org.cafienne.service.db.migration.versions

import org.cafienne.infrastructure.jdbc.{OffsetStoreTables, QueryDbConfig}
import org.cafienne.service.api.cases.table.{CaseTables, CaseTeamMemberRecord}
import org.cafienne.service.api.tasks.TaskTables
import org.cafienne.service.api.tenant.TenantTables
import org.cafienne.service.db.migration.DbSchemaVersion
import slick.lifted
import slick.lifted.TableQuery
import slick.migration.api.TableMigration

trait CaseTablesV1 extends QueryDbConfig with CaseTables {

  import dbConfig.profile.api._

  final class CaseInstanceTeamMemberTableV1(tag: Tag) extends CafienneTable[CaseTeamMemberRecord](tag, "case_instance_team_member") {

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def pk = primaryKey("pk_case_instance_team_member", (caseInstanceId, role, userId))

    def * = (caseInstanceId, tenant, userId, role, active, false, false) <> (CaseTeamMemberRecord.tupled, CaseTeamMemberRecord.unapply)

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def role = idColumn[String]("role")

    def userId = idColumn[String]("user_id")

    def active = column[Boolean]("active")

    def caseInstance =
      foreignKey("fk_case_instance_team_member__case_instance", caseInstanceId, caseInstanceTable)(_.id)
  }

}

object QueryDB_1_0_0 extends DbSchemaVersion
  with TaskTables
  with CaseTablesV1
  with TenantTables
  with OffsetStoreTables {
  val version = "1.0.0"
  val migrations = (
    createTenantTable
      & createTenantOwnersTable
      & createUserRolesTable
      & createTasksTable
      & createCaseInstanceTable
      & createCaseInstanceDefinitionTable
      & createCaseInstanceRoleTable
      & createCaseInstanceTeamMemberTable
      & createPlanItemTable
      & createPlanItemHistoryTable
      & createCaseFileTable
      & createOffsetStoreTable
    )

  def createTenantTable = TableMigration(TableQuery[TenantTable])
    .create
    .addColumns(
      _.name,
      _.enabled
    )

  def createTenantOwnersTable = TableMigration(TableQuery[TenantOwnersTable])
    .create
    .addColumns(
      _.tenant,
      _.userId,
      _.enabled
    )

  def createUserRolesTable = TableMigration(TableQuery[UserRoleTable])
    .create
    .addColumns(
      _.userId,
      _.tenant,
      _.name,
      _.email,
      _.role_name,
      _.enabled)
    .addPrimaryKeys(_.pk)

  def createTasksTable = TableMigration(TableQuery[TaskTable])
    .create
    .addColumns(
      _.id,
      _.caseInstanceId,
      _.tenant,
      //        _.caseDefinition,
      //        _.parentCaseInstanceId,
      //        _.rootCaseInstanceId,
      _.role,
      _.taskName,
      _.taskState,
      _.assignee,
      _.owner,
      _.dueDate,
      _.createdOn,
      _.createdBy,
      _.lastModified,
      _.modifiedBy,
      _.input,
      _.output,
      _.taskModel
    )

  def createCaseInstanceTable = TableMigration(TableQuery[CaseInstanceTable])
    .create
    .addColumns(
      _.id,
      _.tenant,
      _.definition,
      _.state,
      _.failures,
      _.parentCaseId,
      _.rootCaseId,
      _.lastModified,
      _.modifiedBy,
      _.createdBy,
      _.createdOn,
      _.caseInput,
      _.caseOutput
    )

  def createCaseInstanceDefinitionTable = TableMigration(TableQuery[CaseInstanceDefinitionTable])
    .create
    .addColumns(
      _.caseInstanceId,
      _.name,
      _.description,
      _.elementId,
      _.content,
      _.tenant,
      _.lastModified,
      _.modifiedBy
    )

  def createCaseInstanceRoleTable = TableMigration(TableQuery[CaseInstanceRoleTable])
    .create
    .addColumns(
      _.caseInstanceId,
      _.tenant,
      _.roleName,
      _.assigned
    )
    .addPrimaryKeys(_.pk)
    .addForeignKeys(_.caseInstance)

  def createCaseInstanceTeamMemberTable = TableMigration(TableQuery[CaseInstanceTeamMemberTableV1])
    .create
    .addColumns(
      _.userId,
      _.caseInstanceId,
      _.tenant,
      _.role,
      _.active
    )
    .addPrimaryKeys(_.pk)
    .addForeignKeys(_.caseInstance)

  def createPlanItemTable = TableMigration(TableQuery[PlanItemTable])
    .create
    .addColumns(
      _.id,
      _.stageId,
      _.name,
      _.index,
      _.caseInstanceId,
      _.tenant,
      _.currentState,
      _.historyState,
      _.transition,
      _.planItemType,
      _.repeating,
      _.required,
      _.lastModified,
      _.modifiedBy,
      _.createdBy,
      _.createdOn,
      _.taskInput,
      _.taskOutput,
      _.mappedInput,
      _.rawOutput
    )
    .addForeignKeys(_.caseInstance)

  def createPlanItemHistoryTable = TableMigration(TableQuery[PlanItemHistoryTable])
    .create
    .addColumns(
      _.id,
      _.planItemId,
      _.stageId,
      _.name,
      _.index,
      _.caseInstanceId,
      _.tenant,
      _.currentState,
      _.historyState,
      _.transition,
      _.planItemType,
      _.repeating,
      _.required,
      _.lastModified,
      _.modifiedBy,
      _.eventType,
      _.sequenceNr,
      _.taskInput,
      _.taskOutput,
      _.mappedInput,
      _.rawOutput
    )
    .addIndexes(_.idx)

  def createCaseFileTable = TableMigration(TableQuery[CaseFileTable])
    .create
    .addColumns(
      _.caseInstanceId,
      _.tenant,
      _.data,
    )
    .addForeignKeys(_.caseInstance)

  def createOffsetStoreTable = TableMigration(TableQuery[OffsetStoreTable])
    .create
    .addColumns(
      _.name,
      _.offsetType,
      _.offsetValue,
      _.timestamp,
    )
}
