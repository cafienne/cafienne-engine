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

package org.cafienne.querydb.schema.table

import org.cafienne.querydb.record._
import org.cafienne.querydb.schema.QueryDBSchema
import slick.lifted.ColumnOrdered
import slick.relational.RelationalProfile.ColumnOption.Length

import java.time.Instant

trait CaseTables extends QueryDBSchema {

  import dbConfig.profile.api._

  //TODO: add lowercase index on definition in Postgresql to allow case insensitive searching

  class CaseInstanceTable(tag: Tag) extends CafienneTenantTable[CaseRecord](tag, "case_instance") {
    override def getSortColumn(field: String): ColumnOrdered[_] = field match {
      case "id" => id
      case "definition" => caseName // Backwards compatibility; column name before was "definition"
      case "casename" => caseName
      case "name" => caseName
      case "status" => state
      case "state" => state
      case "tenant" => tenant
      case "failures" => failures
      case "parentcaseid" => parentCaseId
      case "rootcaseid" => rootCaseId
      case "modifiedny" => modifiedBy
      case "createdon" => createdOn
      case "createdby" => createdBy
      case "lastmodified" =>  lastModified
      case _ => lastModified
    }

    lazy val id = idColumn[String]("id", O.PrimaryKey)

    lazy val caseName = idColumn[String]("case_name")

    lazy val state = stateColumn[String]("state")

    lazy val failures = column[Int]("failures")

    lazy val parentCaseId = idColumn[String]("parent_case_id")

    lazy val rootCaseId = idColumn[String]("root_case_id")

    lazy val lastModified = column[Instant]("last_modified")

    lazy val modifiedBy = userColumn[String]("modified_by")

    lazy val createdOn = column[Instant]("created_on")

    lazy val createdBy = userColumn[String]("created_by")

    lazy val caseInput = jsonColumn[String]("case_input")

    lazy val caseOutput = jsonColumn[String]("case_output")

    // Some indexes to optimize GetCases queries
    lazy val indexState = oldStyleIndex(state)
    lazy val indexTenant = oldStyleIndex(tenant)
    lazy val indexRootCaseId = oldStyleIndex(rootCaseId)
    lazy val indexCaseName = oldStyleIndex(caseName)
    lazy val indexCreatedBy = oldStyleIndex(createdBy)
    lazy val indexModifiedBy = oldStyleIndex(modifiedBy)

    lazy val * = (id, tenant, caseName, state, failures, parentCaseId, rootCaseId, lastModified, modifiedBy, createdOn, createdBy, caseInput, caseOutput).mapTo[CaseRecord]
  }

  final class CaseInstanceDefinitionTable(tag: Tag) extends CafienneTenantTable[CaseDefinitionRecord](tag, "case_instance_definition") {

    lazy val caseInstanceId = idColumn[String]("caseInstanceId", O.PrimaryKey)

    lazy val name = idColumn[String]("name")

    lazy val description = column[String]("description")

    lazy val elementId = idColumn[String]("element_id")

    lazy val content = column[String]("content")

    lazy val lastModified = column[Instant]("last_modified")

    lazy val modifiedBy = userColumn[String]("modified_by")

    lazy val * = (caseInstanceId, name, description, elementId, content, tenant, lastModified, modifiedBy).mapTo[CaseDefinitionRecord]
  }

  class PlanItemTable(tag: Tag) extends CafienneTenantTable[PlanItemRecord](tag, "plan_item") {

    lazy val id = idColumn[String]("id", O.PrimaryKey)

    lazy val definitionId = idColumn[String]("definition_id", O.Default(""))

    lazy val stageId = idColumn[String]("stage_id")

    lazy val name = column[String]("name")

    lazy val index = column[Int]("index")

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val currentState = stateColumn[String]("current_state")

    lazy val historyState = stateColumn[String]("history_state")

    lazy val transition = stateColumn[String]("transition")

    lazy val planItemType = stateColumn[String]("plan_item_type")

    lazy val repeating = column[Boolean]("repeating")

    lazy val required = column[Boolean]("required")

    lazy val lastModified = column[Instant]("last_modified")

    lazy val modifiedBy = userColumn[String]("modified_by")

    lazy val createdOn = column[Instant]("created_on")

    lazy val createdBy = userColumn[String]("created_by")

    lazy val taskInput = jsonColumn[String]("task_input")

    lazy val taskOutput = jsonColumn[String]("task_output")

    lazy val mappedInput = jsonColumn[String]("mapped_input")

    lazy val rawOutput = jsonColumn[String]("raw_output")

    lazy val * = (id, definitionId, stageId, name, index, caseInstanceId, tenant, currentState, historyState, transition, planItemType, repeating, required, lastModified, modifiedBy, createdOn, createdBy, taskInput, taskOutput, mappedInput, rawOutput).mapTo[PlanItemRecord]

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    lazy val indexCreatedBy = oldStyleIndex(createdBy)
    lazy val indexModifiedBy = oldStyleIndex(modifiedBy)
  }

  class CaseFileTable(tag: Tag) extends CafienneTenantTable[CaseFileRecord](tag, "case_file") {

    lazy val caseInstanceId = idColumn[String]("case_instance_id", O.PrimaryKey)

    lazy val data = jsonColumn[String]("data")

    lazy val * = (caseInstanceId, tenant, data).mapTo[CaseFileRecord]

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
  }

  final class CaseBusinessIdentifierTable(tag: Tag) extends CafienneTenantTable[CaseBusinessIdentifierRecord](tag, "case_business_identifier") {
    override def getSortColumn(field: String): ColumnOrdered[_] = field match {
      case "name" => name
      case "tenant" => tenant
      case "value" => value
      case "caseInstanceId" => caseInstanceId
      case _ => name
    }

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val name = idColumn[String]("name")

    lazy val value = column[Option[String]]("value")

    lazy val active = column[Boolean]("active")

    lazy val path = column[String]("path")

    lazy val * = (caseInstanceId, tenant, name, value, active, path).mapTo[CaseBusinessIdentifierRecord]

    lazy val caseInstanceTable = TableQuery[CaseInstanceTable]

    lazy val pk = primaryKey(pkName, (caseInstanceId, name))

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    lazy val indexName = oldStyleIndex(name)
  }

  class CaseInstanceRoleTable(tag: Tag) extends CafienneTenantTable[CaseRoleRecord](tag, "case_instance_role") {

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val roleName = idColumn[String]("role_name")

    lazy val assigned = column[Boolean]("assigned") // true if team members are assigned for this role

    lazy val pk = primaryKey(pkName, (caseInstanceId, roleName))

    lazy val * = (caseInstanceId, tenant, roleName, assigned).mapTo[CaseRoleRecord]

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
  }

  class CaseInstanceTeamUserTable(tag: Tag) extends CafienneTenantTable[CaseTeamUserRecord](tag, "case_instance_team_user") {

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val userId = userColumn[String]("user_id")

    lazy val origin = column[String]("origin", Length(32), O.Default(""))

    lazy val caseRole = idColumn[String]("case_role")

    lazy val isOwner = column[Boolean]("isOwner")

    lazy val pk = primaryKey(pkName, (caseInstanceId, caseRole, userId))

    lazy val * = (caseInstanceId, tenant, userId, origin, caseRole, isOwner).mapTo[CaseTeamUserRecord]

    lazy val indexCaseInstanceId = index(caseInstanceId)
    lazy val indexUserId = index(userId)
  }

  class CaseInstanceTeamTenantRoleTable(tag: Tag) extends CafienneTenantTable[CaseTeamTenantRoleRecord](tag, "case_instance_team_tenant_role") {

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val tenantRole = userColumn[String]("tenant_role")

    lazy val caseRole = idColumn[String]("case_role")

    lazy val isOwner = column[Boolean]("isOwner")

    lazy val pk = primaryKey(pkName, (caseInstanceId, tenant, tenantRole, caseRole))

    lazy val * = (caseInstanceId, tenant, tenantRole, caseRole, isOwner).mapTo[CaseTeamTenantRoleRecord]

    lazy val indexCaseInstanceId = index(caseInstanceId)
    lazy val indexTenantRoles = index(ixName(tenantRole), (tenant, tenantRole))
  }

  class CaseInstanceTeamGroupTable(tag: Tag) extends CafienneTenantTable[CaseTeamGroupRecord](tag, "case_instance_team_group") {

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val groupId = idColumn[String]("group_id")

    lazy val groupRole = idColumn[String]("group_role")

    lazy val caseRole = idColumn[String]("case_role")

    lazy val isOwner = column[Boolean]("isOwner")

    lazy val pk = primaryKey(pkName, (caseInstanceId, groupId, groupRole, caseRole))

    lazy val * = (caseInstanceId, tenant, groupId, groupRole, caseRole, isOwner).mapTo[CaseTeamGroupRecord]

    lazy val indexCaseInstanceId = index(caseInstanceId)
    lazy val indexGroupId = index(groupId)
    lazy val indexCaseGroups = index(s"ix_case_id_group_id__$tableName", (caseInstanceId, groupId))
    lazy val indexGroupMemberRole = index(ixName(groupRole), (caseInstanceId, groupId, groupRole))
  }
}
