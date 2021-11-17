package org.cafienne.service.db.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.service.db.schema.QueryDBSchema
import org.cafienne.service.db.schema.table.CaseTables
import org.cafienne.service.db.schema.versions.util.Projections
import slick.migration.api.TableMigration

object QueryDB_1_1_16 extends DbSchemaVersion with QueryDBSchema
  with CafienneTablesV2
  with CaseTables {

  val version = "1.1.16"
  val migrations = Projections.renameOffsets
      .&(createCaseTeamUserTable)
      .&(createCaseTeamTenantRoleTable)
      .&(fillCaseTeamTenantRoleTable)
      .&(fillCaseTeamUserTable)
      .&(dropCaseTeamMemberTable)

  import dbConfig.profile.api._

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
}
