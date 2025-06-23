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

import org.cafienne.persistence.querydb.record._
import slick.lifted.ColumnOrdered

import java.time.Instant

trait CaseTables extends CaseTeamTables {

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
      case "lastmodified" => lastModified
      case _ => lastModified
    }

    lazy val id: Rep[String] = idColumn[String]("id", O.PrimaryKey)
    lazy val caseName: Rep[String] = idColumn[String]("case_name")
    lazy val state: Rep[String] = stateColumn[String]("state")
    lazy val failures: Rep[Int] = column[Int]("failures")
    lazy val parentCaseId: Rep[String] = idColumn[String]("parent_case_id")
    lazy val rootCaseId: Rep[String] = idColumn[String]("root_case_id")
    lazy val lastModified: Rep[Instant] = column[Instant]("last_modified")
    lazy val modifiedBy: Rep[String] = userColumn[String]("modified_by")
    lazy val createdOn: Rep[Instant] = column[Instant]("created_on")
    lazy val createdBy: Rep[String] = userColumn[String]("created_by")
    lazy val caseInput: Rep[String] = jsonColumn[String]("case_input")
    lazy val caseOutput: Rep[String] = jsonColumn[String]("case_output")

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

    lazy val caseInstanceId: Rep[String] = idColumn[String]("caseInstanceId", O.PrimaryKey)
    lazy val name: Rep[String] = idColumn[String]("name")
    lazy val description: Rep[String] = column[String]("description")
    lazy val elementId: Rep[String] = idColumn[String]("element_id")
    lazy val content: Rep[String] = column[String]("content")
    lazy val lastModified: Rep[Instant] = column[Instant]("last_modified")
    lazy val modifiedBy: Rep[String] = userColumn[String]("modified_by")

    lazy val * = (caseInstanceId, name, description, elementId, content, tenant, lastModified, modifiedBy).mapTo[CaseDefinitionRecord]
  }

  class PlanItemTable(tag: Tag) extends CafienneTenantTable[PlanItemRecord](tag, "plan_item") {

    lazy val id: Rep[String] = idColumn[String]("id", O.PrimaryKey)
    lazy val definitionId: Rep[String] = idColumn[String]("definition_id", O.Default(""))
    lazy val stageId: Rep[String] = idColumn[String]("stage_id")
    lazy val name: Rep[String] = column[String]("name")
    lazy val index: Rep[Int] = column[Int]("index")
    lazy val caseInstanceId: Rep[String] = idColumn[String]("case_instance_id")
    lazy val currentState: Rep[String] = stateColumn[String]("current_state")
    lazy val historyState: Rep[String] = stateColumn[String]("history_state")
    lazy val transition: Rep[String] = stateColumn[String]("transition")
    lazy val planItemType: Rep[String] = stateColumn[String]("plan_item_type")
    lazy val repeating: Rep[Boolean] = column[Boolean]("repeating")
    lazy val required: Rep[Boolean] = column[Boolean]("required")
    lazy val lastModified: Rep[Instant] = column[Instant]("last_modified")
    lazy val modifiedBy: Rep[String] = userColumn[String]("modified_by")
    lazy val createdOn: Rep[Instant] = column[Instant]("created_on")
    lazy val createdBy: Rep[String] = userColumn[String]("created_by")
    lazy val taskInput: Rep[String] = jsonColumn[String]("task_input")
    lazy val taskOutput: Rep[String] = jsonColumn[String]("task_output")
    lazy val mappedInput: Rep[String] = jsonColumn[String]("mapped_input")
    lazy val rawOutput: Rep[String] = jsonColumn[String]("raw_output")

    lazy val * = (id, definitionId, stageId, name, index, caseInstanceId, tenant, currentState, historyState, transition, planItemType, repeating, required, lastModified, modifiedBy, createdOn, createdBy, taskInput, taskOutput, mappedInput, rawOutput).mapTo[PlanItemRecord]

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    lazy val indexCreatedBy = oldStyleIndex(createdBy)
    lazy val indexModifiedBy = oldStyleIndex(modifiedBy)
  }

  class CaseFileTable(tag: Tag) extends CafienneTenantTable[CaseFileRecord](tag, "case_file") {

    lazy val caseInstanceId: Rep[String] = idColumn[String]("case_instance_id", O.PrimaryKey)
    lazy val data: Rep[String] = jsonColumn[String]("data")

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

    lazy val caseInstanceId: Rep[String] = idColumn[String]("case_instance_id")
    lazy val name: Rep[String] = idColumn[String]("name")
    lazy val value: Rep[Option[String]] = column[Option[String]]("value")
    lazy val active: Rep[Boolean] = column[Boolean]("active")
    lazy val path: Rep[String] = column[String]("path")

    lazy val * = (caseInstanceId, tenant, name, value, active, path).mapTo[CaseBusinessIdentifierRecord]

    lazy val pk = primaryKey(pkName, (caseInstanceId, name))

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    lazy val indexName = oldStyleIndex(name)
  }

  class CaseInstanceRoleTable(tag: Tag) extends CafienneTenantTable[CaseRoleRecord](tag, "case_instance_role") {

    lazy val caseInstanceId: Rep[String] = idColumn[String]("case_instance_id")
    lazy val roleName: Rep[String] = idColumn[String]("role_name")
    lazy val assigned: Rep[Boolean] = column[Boolean]("assigned") // true if team members are assigned for this role

    lazy val pk = primaryKey(pkName, (caseInstanceId, roleName))

    lazy val * = (caseInstanceId, tenant, roleName, assigned).mapTo[CaseRoleRecord]

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
  }
}
