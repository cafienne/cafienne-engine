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

package org.cafienne.persistence.querydb.schema.versions

import org.cafienne.persistence.infrastructure.jdbc.schema.QueryDBSchemaVersion
import org.cafienne.persistence.querydb.schema.table.{CaseTables, ConsentGroupTables}
import org.cafienne.persistence.querydb.schema.versions.util.Projections
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.migration.api.TableMigration

class QueryDB_1_1_16(val dbConfig: DatabaseConfig[JdbcProfile], val tablePrefix: String)
  extends QueryDBSchemaVersion
    with CafienneTablesV2
    with ConsentGroupTables
    with CaseTables {

  val version = "1.1.16"
  val migrations = new Projections(dbConfig, tablePrefix).renameOffsets
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
