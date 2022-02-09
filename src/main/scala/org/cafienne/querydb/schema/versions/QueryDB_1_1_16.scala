package org.cafienne.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.{CaseTables, ConsentGroupTables}
import org.cafienne.querydb.schema.versions.util.Projections
import slick.migration.api.TableMigration

object QueryDB_1_1_16 extends DbSchemaVersion with QueryDBSchema
  with CafienneTablesV2
  with ConsentGroupTables
  with CaseTables {

  val version = "1.1.16"
  val migrations = Projections.renameOffsets
      .&(createConsentGroupTable)
      .&(createConsentGroupMemberTable)
      .&(createCaseTeamUserTable)
      .&(createCaseTeamTenantRoleTable)
      .&(fillCaseTeamTenantRoleTable)
      .&(fillCaseTeamUserTable)
      .&(dropCaseTeamMemberTable)
      .&(createCaseTeamGroupTable)

  import dbConfig.profile.api._

  def createConsentGroupTable = TableMigration(TableQuery[ConsentGroupTable])
    .create
    .addColumns(
      _.id,
      _.tenant
    )

  def createConsentGroupMemberTable = TableMigration(TableQuery[ConsentGroupMemberTable])
    .create
    .addColumns(
      _.userId,
      _.group,
      _.role,
      _.isOwner)
    .addPrimaryKeys(_.pk)

  def createCaseTeamUserTable = TableMigration(TableQuery[CaseInstanceTeamUserTable])
    .create
    .addColumns(
      _.caseInstanceId,
      _.tenant,
      _.userId,
      _.origin,
      _.caseRole,
      _.isOwner,
    )
    .addPrimaryKeys(_.pk)
    .addIndexes(_.indexCaseInstanceId, _.indexUserId)

  def createCaseTeamTenantRoleTable = TableMigration(TableQuery[CaseInstanceTeamTenantRoleTable])
    .create
    .addColumns(
      _.caseInstanceId,
      _.tenant,
      _.tenantRole,
      _.caseRole,
      _.isOwner,
    )
    .addPrimaryKeys(_.pk)
    .addIndexes(_.indexCaseInstanceId, _.indexTenantRoles)

  def fillCaseTeamUserTable = {
    val userMembers = TableQuery[CaseInstanceTeamMemberTable].filter(_.active).filter(_.isTenantUser).map(u => (u.caseInstanceId, u.tenant, u.memberId, "tenant", u.caseRole, u.isOwner))
    asSqlMigration(TableQuery[CaseInstanceTeamUserTable].map(u => (u.caseInstanceId, u.tenant, u.userId, u.origin, u.caseRole, u.isOwner)) forceInsertQuery userMembers)
  }

  def fillCaseTeamTenantRoleTable = {
    val tenantRoleMembers = TableQuery[CaseInstanceTeamMemberTable].filter(_.active).filterNot(_.isTenantUser).map(u => (u.caseInstanceId, u.tenant, u.memberId, u.caseRole, u.isOwner))
    asSqlMigration(TableQuery[CaseInstanceTeamTenantRoleTable].map(u => (u.caseInstanceId, u.tenant, u.tenantRole, u.caseRole, u.isOwner)) forceInsertQuery tenantRoleMembers)
  }

  def dropCaseTeamMemberTable = {
    TableMigration(TableQuery[CaseInstanceTeamMemberTable]).drop
  }

  def createCaseTeamGroupTable = TableMigration(TableQuery[CaseInstanceTeamGroupTable])
    .create
    .addColumns(
      _.caseInstanceId,
      _.tenant,
      _.groupId,
      _.groupRole,
      _.caseRole,
      _.isOwner
    )
    .addPrimaryKeys(_.pk)
    .addIndexes(_.indexCaseInstanceId, _.indexCaseGroups, _.indexGroupMemberRole)
}
