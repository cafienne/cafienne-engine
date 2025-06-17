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

package org.cafienne.persistence.querydb.schema.table

import org.cafienne.persistence.infrastructure.jdbc.SlickTableExtensions
import org.cafienne.persistence.querydb.record._
import slick.relational.RelationalProfile.ColumnOption.Length

trait CaseTeamTables extends SlickTableExtensions {

  import dbConfig.profile.api._

  /**
   * Simple base abstraction to enable correct re-use of fields
   */
  abstract class CaseInstanceTeamMembershipTable[R](tag: Tag, tableName: String) extends CafienneTenantTable[R](tag, tableName) {

    lazy val caseInstanceId: Rep[String] = idColumn[String]("case_instance_id")
    lazy val caseRole: Rep[String] = idColumn[String]("case_role")
    lazy val isOwner: Rep[Boolean] = column[Boolean]("isOwner")

    lazy val indexCaseInstanceId = index(caseInstanceId)
  }

  class CaseInstanceTeamUserTable(tag: Tag) extends CaseInstanceTeamMembershipTable[CaseTeamUserRecord](tag, "case_instance_team_user") {

    lazy val userId: Rep[String] = userColumn[String]("user_id")
    lazy val origin: Rep[String] = column[String]("origin", Length(32), O.Default(""))

    lazy val pk = primaryKey(pkName, (caseInstanceId, caseRole, userId))

    lazy val * = (caseInstanceId, tenant, userId, origin, caseRole, isOwner).mapTo[CaseTeamUserRecord]

    lazy val indexUserId = index(userId)
  }

  class CaseInstanceTeamTenantRoleTable(tag: Tag) extends CaseInstanceTeamMembershipTable[CaseTeamTenantRoleRecord](tag, "case_instance_team_tenant_role") {

    lazy val tenantRole: Rep[String] = userColumn[String]("tenant_role")

    lazy val pk = primaryKey(pkName, (caseInstanceId, tenant, tenantRole, caseRole))

    lazy val * = (caseInstanceId, tenant, tenantRole, caseRole, isOwner).mapTo[CaseTeamTenantRoleRecord]

    lazy val indexTenantRoles = index(ixName(tenantRole), (tenant, tenantRole))
  }

  class CaseInstanceTeamGroupTable(tag: Tag) extends CaseInstanceTeamMembershipTable[CaseTeamGroupRecord](tag, "case_instance_team_group") {

    lazy val groupId: Rep[String] = idColumn[String]("group_id")
    lazy val groupRole: Rep[String] = idColumn[String]("group_role")

    lazy val pk = primaryKey(pkName, (caseInstanceId, groupId, groupRole, caseRole))

    lazy val * = (caseInstanceId, tenant, groupId, groupRole, caseRole, isOwner).mapTo[CaseTeamGroupRecord]

    lazy val indexCaseGroups = index(s"ix_case_id_group_id__$tableName", (caseInstanceId, groupId))
    lazy val indexGroupMemberRole = index(ixName(groupRole), (caseInstanceId, groupId, groupRole))
  }
}
