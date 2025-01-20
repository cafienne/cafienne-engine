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

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.persistence.querydb.schema.QueryDBSchema
import org.cafienne.persistence.querydb.schema.table.{CaseTables, TaskTables, TenantTables}
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
