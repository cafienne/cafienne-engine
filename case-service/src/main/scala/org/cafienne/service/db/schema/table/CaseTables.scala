package org.cafienne.service.db.schema.table

import org.cafienne.service.db.record._
import org.cafienne.service.db.schema.QueryDBSchema
import slick.lifted.ColumnOrdered

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

  final class PlanItemHistoryTable(tag: Tag) extends CafienneTenantTable[PlanItemHistoryRecord](tag, "plan_item_history") {

    lazy val id = idColumn[String]("id", O.PrimaryKey)

    lazy val planItemId = idColumn[String]("plan_item_id")

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

    lazy val eventType = column[String]("eventType")

    lazy val sequenceNr = column[Long]("sequenceNr")

    lazy val taskInput = jsonColumn[String]("task_input")

    lazy val taskOutput = jsonColumn[String]("task_output")

    lazy val mappedInput = jsonColumn[String]("mapped_input")

    lazy val rawOutput = jsonColumn[String]("raw_output")

    lazy val * = (id, planItemId, stageId, name, index, caseInstanceId, tenant, currentState, historyState,transition, planItemType, repeating, required, lastModified, modifiedBy, eventType, sequenceNr, taskInput, taskOutput, mappedInput, rawOutput).mapTo[PlanItemHistoryRecord]

    lazy val idx = index("idx_plan_item_history__plain_item_id", planItemId)
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

  class CaseInstanceTeamMemberTable(tag: Tag) extends CafienneTenantTable[CaseTeamMemberRecord](tag, "case_instance_team_member") {

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val caseRole = idColumn[String]("case_role")

    lazy val memberId = userColumn[String]("member_id")

    lazy val isTenantUser = column[Boolean]("isTenantUser")

    lazy val isOwner = column[Boolean]("isOwner")

    lazy val active = column[Boolean]("active")

    lazy val pk = primaryKey(pkName, (caseInstanceId, caseRole, memberId, isTenantUser))

    lazy val * = (caseInstanceId, tenant, memberId, caseRole, isTenantUser, isOwner, active).mapTo[CaseTeamMemberRecord]

    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    lazy val indexMemberId = index(oldStyleIxName(memberId), (memberId, isTenantUser))
  }
}
