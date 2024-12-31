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
